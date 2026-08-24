package com.microsoft.aediumbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.microsoft.aediumbackend.commen.CursorPage;
import com.microsoft.aediumbackend.commen.CursorPageRequest;
import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.exception.BusinessException;
import com.microsoft.aediumbackend.mapper.UserMapper;
import com.microsoft.aediumbackend.mapper.UserFollowMapper;
import com.microsoft.aediumbackend.model.dto.follow.FollowActionRequest;
import com.microsoft.aediumbackend.model.dto.follow.FollowBatchStatusRequest;
import com.microsoft.aediumbackend.model.dto.follow.FollowBatchStatusResult;
import com.microsoft.aediumbackend.model.entity.Notification;
import com.microsoft.aediumbackend.model.entity.User;
import com.microsoft.aediumbackend.model.entity.UserFollow;
import com.microsoft.aediumbackend.model.enums.FollowAction;
import com.microsoft.aediumbackend.model.enums.NotificationTargetType;
import com.microsoft.aediumbackend.model.enums.NotificationType;
import com.microsoft.aediumbackend.model.vo.UserVO;
import com.microsoft.aediumbackend.utils.CursorPageUtils;
import com.microsoft.aediumbackend.utils.CurrentHold;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.*;

/**
 * 用户关注服务，负责维护关注关系、关注计数及批量关注状态。
 */
@Service
public class UserFollowService {

    private static final int FOLLOW_LIMIT = 5000;

    private final UserFollowMapper userFollowMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    /**
     * 创建用户关注服务。
     *
     * @param userFollowMapper    关注关系 Mapper
     * @param userMapper          用户 Mapper
     * @param notificationService 通知服务
     */
    public UserFollowService(
            UserFollowMapper userFollowMapper,
            UserMapper userMapper,
            NotificationService notificationService
    ) {
        this.userFollowMapper = userFollowMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    /**
     * 处理关注或取消关注操作。
     *
     * @param request 关注操作参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void handleAction(FollowActionRequest request) {
        Long currentUserId = CurrentHold.getCurrentId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.NO_AUTH, PARAM_INVALID);
        }
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_EMPTY);
        }

        Long targetUserId = request.getTargetUserId();
        if (targetUserId == null || targetUserId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, FOLLOW_TARGET_USER_ID_INVALID);
        }
        FollowAction action = FollowAction.getByCode(request.getAction());
        if (action == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, FOLLOW_ACTION_INVALID);
        }
        if (currentUserId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, FOLLOW_SELF_NOT_ALLOWED);
        }

        Map<Long, User> lockedUsers = lockUsers(currentUserId, targetUserId);
        User currentUser = lockedUsers.get(currentUserId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH, USER_NOT_FOUND);
        }
        User targetUser = lockedUsers.get(targetUserId);
        if (targetUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, FOLLOW_TARGET_NOT_FOUND);
        }

        boolean following = isFollowing(currentUserId, targetUserId);
        if (action == FollowAction.FOLLOW) {
            follow(currentUser, targetUser, following);
        } else {
            unfollow(currentUserId, targetUserId, following);
        }
    }

    /**
     * 按用户 ID 升序锁定当前用户和目标用户，避免并发计数错误及死锁。
     *
     * @param currentUserId 当前用户 ID
     * @param targetUserId  目标用户 ID
     * @return 按用户 ID 索引的用户信息
     */
    private Map<Long, User> lockUsers(Long currentUserId, Long targetUserId) {
        List<Long> userIds = currentUserId < targetUserId
                ? List.of(currentUserId, targetUserId)
                : List.of(targetUserId, currentUserId);
        return userMapper.selectList(
                        new QueryWrapper<User>()
                                .in("id", userIds)
                                .orderByAsc("id")
                                .last("FOR UPDATE")
                ).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
    }

    /**
     * 查询当前用户是否已关注目标用户。
     *
     * @param currentUserId 当前用户 ID
     * @param targetUserId  目标用户 ID
     * @return 是否已关注
     */
    private boolean isFollowing(Long currentUserId, Long targetUserId) {
        return userFollowMapper.selectCount(
                new QueryWrapper<UserFollow>()
                        .eq("follower_id", currentUserId)
                        .eq("followed_id", targetUserId)
        ) > 0;
    }

    /**
     * 新增关注关系，并同步增加双方计数和创建首次关注通知。
     *
     * @param currentUser 当前用户
     * @param targetUser  目标用户
     * @param following   是否已关注
     */
    private void follow(User currentUser, User targetUser, boolean following) {
        if (following) {
            return;
        }
        if (currentUser.getFollowingCount() != null
                && currentUser.getFollowingCount() >= FOLLOW_LIMIT) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, FOLLOW_LIMIT_EXCEEDED);
        }

        UserFollow userFollow = new UserFollow();
        userFollow.setFollowerId(currentUser.getId());
        userFollow.setFollowedId(targetUser.getId());
        if (userFollowMapper.insert(userFollow) != 1) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, DATABASE_INSERT_FAILED);
        }

        incrementFollowCounts(currentUser.getId(), targetUser.getId());
        createNotificationIfAbsent(currentUser.getId(), targetUser.getId());
    }

    /**
     * 物理删除关注关系，并同步减少双方计数。
     *
     * @param currentUserId 当前用户 ID
     * @param targetUserId  目标用户 ID
     * @param following     是否已关注
     */
    private void unfollow(Long currentUserId, Long targetUserId, boolean following) {
        if (!following) {
            return;
        }

        int deleted = userFollowMapper.delete(
                new QueryWrapper<UserFollow>()
                        .eq("follower_id", currentUserId)
                        .eq("followed_id", targetUserId)
        );
        if (deleted != 1) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, DATABASE_DELETE_FAILED);
        }
        decrementFollowCounts(currentUserId, targetUserId);
    }

    /**
     * 增加当前用户的关注数和目标用户的粉丝数。
     *
     * @param currentUserId 当前用户 ID
     * @param targetUserId  目标用户 ID
     */
    private void incrementFollowCounts(Long currentUserId, Long targetUserId) {
        int followingUpdated = userMapper.update(
                null,
                new UpdateWrapper<User>()
                        .eq("id", currentUserId)
                        .setSql("following_count = following_count + 1")
        );
        int followerUpdated = userMapper.update(
                null,
                new UpdateWrapper<User>()
                        .eq("id", targetUserId)
                        .setSql("follower_count = follower_count + 1")
        );
        if (followingUpdated != 1 || followerUpdated != 1) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, DATABASE_UPDATE_FAILED);
        }
    }

    /**
     * 减少当前用户的关注数和目标用户的粉丝数，计数最低为 0。
     *
     * @param currentUserId 当前用户 ID
     * @param targetUserId  目标用户 ID
     */
    private void decrementFollowCounts(Long currentUserId, Long targetUserId) {
        int followingUpdated = userMapper.update(
                null,
                new UpdateWrapper<User>()
                        .eq("id", currentUserId)
                        .setSql("following_count = GREATEST(following_count - 1, 0)")
        );
        int followerUpdated = userMapper.update(
                null,
                new UpdateWrapper<User>()
                        .eq("id", targetUserId)
                        .setSql("follower_count = GREATEST(follower_count - 1, 0)")
        );
        if (followingUpdated != 1 || followerUpdated != 1) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, DATABASE_UPDATE_FAILED);
        }
    }

    /**
     * 首次建立关注关系时创建关注通知。
     *
     * @param currentUserId 当前用户 ID
     * @param targetUserId  目标用户 ID
     */
    private void createNotificationIfAbsent(Long currentUserId, Long targetUserId) {
        long notificationCount = notificationService.count(
                new QueryWrapper<Notification>()
                        .eq("recipient_id", targetUserId)
                        .eq("actor_id", currentUserId)
                        .eq("type", NotificationType.NEW_FOLLOWER.getValue())
                        .eq("target_type", NotificationTargetType.USER.getValue())
                        .eq("target_id", currentUserId)
        );
        if (notificationCount == 0) {
            notificationService.createNotification(
                    targetUserId,
                    currentUserId,
                    NotificationType.NEW_FOLLOWER.getValue(),
                    NotificationTargetType.USER.getValue(),
                    currentUserId,
                    null
            );
        }
    }

    /**
     * 批量查询当前用户对目标用户的关注状态。
     *
     * @param request 批量查询参数
     * @return 每个目标用户对应的关注状态
     */
    public FollowBatchStatusResult batchStatus(FollowBatchStatusRequest request) {
        Long currentUserId = CurrentHold.getCurrentId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.NO_AUTH, PARAM_INVALID);
        }

        Set<Long> targetUserIds = new LinkedHashSet<>(request.getTargetUserIds());
        List<UserFollow> follows = userFollowMapper.selectList(
                new QueryWrapper<UserFollow>()
                        .select("followed_id")
                        .eq("follower_id", currentUserId)
                        .in("followed_id", targetUserIds)
        );
        Set<Long> followedUserIds = follows.stream()
                .map(UserFollow::getFollowedId)
                .collect(Collectors.toSet());

        Map<Long, Boolean> followingMap = new LinkedHashMap<>();
        targetUserIds.forEach(targetUserId ->
                followingMap.put(targetUserId, followedUserIds.contains(targetUserId)));
        return new FollowBatchStatusResult(followingMap);
    }

    /**
     * 分页查询指定用户的粉丝列表。
     *
     * @param userId  被查看用户 ID
     * @param request 游标分页参数
     * @return 粉丝用户分页
     */
    public CursorPage<UserVO> getFollowers(Long userId, CursorPageRequest request) {
        return getFollowUsers(userId, request, true);
    }

    /**
     * 分页查询指定用户的关注列表。
     *
     * @param userId  被查看用户 ID
     * @param request 游标分页参数
     * @return 被关注用户分页
     */
    public CursorPage<UserVO> getFollowing(Long userId, CursorPageRequest request) {
        return getFollowUsers(userId, request, false);
    }

    /**
     * 查询关注关系并聚合用户信息。
     */
    private CursorPage<UserVO> getFollowUsers(
            Long userId,
            CursorPageRequest request,
            boolean queryFollowers
    ) {
        if (CurrentHold.getCurrentId() == null) {
            throw new BusinessException(ErrorCode.NO_AUTH, PARAM_INVALID);
        }
        validateFollowListRequest(userId, request);

        String ownerColumn = queryFollowers ? "followed_id" : "follower_id";
        String listUserColumn = queryFollowers ? "follower_id" : "followed_id";
        Function<UserFollow, Long> userIdExtractor = queryFollowers
                ? UserFollow::getFollowerId
                : UserFollow::getFollowedId;

        QueryWrapper<UserFollow> queryWrapper = new QueryWrapper<UserFollow>()
                .select(ownerColumn, listUserColumn, "create_time")
                .eq(ownerColumn, userId);
        if (request.getLastCreatedAt() != null) {
            queryWrapper.and(wrapper -> wrapper
                    .lt("create_time", request.getLastCreatedAt())
                    .or(nested -> nested
                            .eq("create_time", request.getLastCreatedAt())
                            .lt(listUserColumn, request.getLastId())));
        }
        queryWrapper
                .orderByDesc("create_time")
                .orderByDesc(listUserColumn)
                .last("LIMIT " + (request.getSize() + 1));

        List<UserFollow> follows = userFollowMapper.selectList(queryWrapper);
        if (follows.isEmpty()) {
            return new CursorPage<>(Collections.emptyList(), false, null, null);
        }

        CursorPageUtils.CursorInfo cursorInfo = CursorPageUtils.extract(
                follows,
                request.getSize(),
                UserFollow::getCreateTime,
                userIdExtractor
        );
        Set<Long> userIds = follows.stream()
                .map(userIdExtractor)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<User> users = userMapper.selectList(
                new QueryWrapper<User>()
                        .in("id", userIds)
                        .eq("is_delete", 0)
        );
        Map<Long, UserVO> usersById = users.isEmpty()
                ? Collections.emptyMap()
                : users.stream()
                        .map(UserVO::getUserVO)
                        .collect(Collectors.toMap(UserVO::getId, user -> user));
        List<UserVO> items = follows.stream()
                .map(userIdExtractor)
                .map(usersById::get)
                .filter(Objects::nonNull)
                .toList();

        return new CursorPage<>(
                items,
                cursorInfo.isHasMore(),
                cursorInfo.getNextCursorCreatedAt(),
                cursorInfo.getNextCursorId()
        );
    }

    /**
     * 校验关注列表查询参数。
     */
    private void validateFollowListRequest(Long userId, CursorPageRequest request) {
        if (userId == null || userId <= 0 || request == null
                || request.getSize() < 1 || request.getSize() > 50) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_FORMAT_ERROR);
        }
        boolean missingCreatedAt = request.getLastCreatedAt() == null;
        boolean missingId = request.getLastId() == null;
        if (missingCreatedAt != missingId
                || (!missingId && request.getLastId() <= 0)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_FORMAT_ERROR);
        }
    }
}
