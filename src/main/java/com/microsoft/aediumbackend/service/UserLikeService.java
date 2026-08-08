package com.microsoft.aediumbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.exception.BusinessException;
import com.microsoft.aediumbackend.mapper.ArticleMapper;
import com.microsoft.aediumbackend.mapper.CommentMapper;
import com.microsoft.aediumbackend.mapper.UserLikeMapper;
import com.microsoft.aediumbackend.model.dto.like.LikeActionRequest;
import com.microsoft.aediumbackend.model.dto.like.LikeBatchStatusRequest;
import com.microsoft.aediumbackend.model.dto.like.LikeBatchStatusResult;
import com.microsoft.aediumbackend.model.entity.Article;
import com.microsoft.aediumbackend.model.entity.Comment;
import com.microsoft.aediumbackend.model.entity.Notification;
import com.microsoft.aediumbackend.model.entity.UserLike;
import com.microsoft.aediumbackend.model.enums.LikeAction;
import com.microsoft.aediumbackend.model.enums.LikeTargetType;
import com.microsoft.aediumbackend.model.enums.NotificationTargetType;
import com.microsoft.aediumbackend.model.enums.NotificationType;
import com.microsoft.aediumbackend.service.impl.like.LikePushService;
import com.microsoft.aediumbackend.utils.CurrentHold;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.*;

@Service
@Slf4j
public class UserLikeService {

    @Resource
    private UserLikeMapper userLikeMapper;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private CommentMapper commentMapper;
    @Resource
    private NotificationService notificationService;
    @Resource
    private LikePushService likePushService;

    @Transactional(rollbackFor = Exception.class)
    public void handleAction(LikeActionRequest request) {
        Long userId = CurrentHold.getCurrentId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.NO_AUTH, PARAM_INVALID);
        }
        if (request.getTargetType() == null || request.getTargetId() == null || request.getAction() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_EMPTY);
        }

        LikeTargetType targetType = LikeTargetType.getByCode(request.getTargetType());
        LikeAction action = LikeAction.getByCode(request.getAction());
        if (targetType == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, LIKE_TARGET_TYPE_INVALID);
        }
        if (action == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, LIKE_ACTION_INVALID);
        }

        Notification notification = null;
        switch (action) {
            case LIKE -> notification = doLike(userId, targetType, request.getTargetId());
            case UNLIKE -> doUnlike(userId, targetType, request.getTargetId());
        }

        // 通知推送必须异步，且必须在事务提交后触发：否则推送线程查不到未提交的通知数据
        if (notification != null) {
            registerAfterCommitPush(notification);
        }
    }

    /**
     * 注册事务提交后回调以触发推送
     * handleAction 是事务入口，正常情况下一定有活跃事务；
     * 若无事务上下文则降级为直接异步推送（数据已可见）
     */
    private void registerAfterCommitPush(Notification notification) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            likePushService.pushLikeNotification(notification);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                likePushService.pushLikeNotification(notification);
            }
        });
    }

    private Notification doLike(Long userId, LikeTargetType targetType, Long targetId) {
        // 不能点赞自己的文章（评论不限制）
        if (targetType == LikeTargetType.ARTICLE) {
            Article article = articleMapper.selectById(targetId);
            if (article == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, LIKE_TARGET_NOT_FOUND);
            }
            if (article.getAuthorId().equals(userId)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, LIKE_OWN_ARTICLE_NOT_ALLOWED);
            }
        }

        UserLike existing = userLikeMapper.findByUserAndTarget(userId, targetType.getCode(), targetId);

        if (existing == null) {
            UserLike userLike = new UserLike();
            userLike.setUserId(userId);
            userLike.setTargetType(targetType.getCode());
            userLike.setTargetId(targetId);
            userLike.setIsDeleted(0);
            userLikeMapper.insert(userLike);
            incrementCount(targetType, targetId);
            return createNotificationIfAbsent(userId, targetType, targetId);
        } else if (existing.getIsDeleted() == 1) {
            userLikeMapper.restoreLike(existing.getId());
            incrementCount(targetType, targetId);
            return createNotificationIfAbsent(userId, targetType, targetId);
        }
        return null;
    }

    private void doUnlike(Long userId, LikeTargetType targetType, Long targetId) {
        UserLike existing = userLikeMapper.findByUserAndTarget(userId, targetType.getCode(), targetId);

        if (existing != null && existing.getIsDeleted() == 0) {
            userLikeMapper.cancelLike(existing.getId());
            decrementCount(targetType, targetId);
        }
    }

    private void incrementCount(LikeTargetType targetType, Long targetId) {
        switch (targetType) {
            case ARTICLE -> articleMapper.incrementLikeCount(targetId);
            case COMMENT -> commentMapper.incrementLikeCount(targetId);
        }
    }

    private void decrementCount(LikeTargetType targetType, Long targetId) {
        switch (targetType) {
            case ARTICLE -> articleMapper.decrementLikeCount(targetId);
            case COMMENT -> commentMapper.decrementLikeCount(targetId);
        }
    }

    private Notification createNotificationIfAbsent(Long actorId, LikeTargetType targetType, Long targetId) {
        NotificationType notificationType = targetType == LikeTargetType.ARTICLE
                ? NotificationType.LIKE_ARTICLE : NotificationType.LIKE_COMMENT;
        NotificationTargetType notificationTargetType = targetType == LikeTargetType.ARTICLE
                ? NotificationTargetType.ARTICLE : NotificationTargetType.COMMENT;

        Long recipientId;
        Map<String, Object> params = null;

        if (targetType == LikeTargetType.ARTICLE) {
            Article article = articleMapper.selectById(targetId);
            recipientId = article != null ? article.getAuthorId() : null;
        } else {
            Comment comment = commentMapper.selectById(targetId);
            recipientId = comment != null ? comment.getUserId() : null;
            if (comment != null) {
                params = Map.of("articleId", comment.getArticleId());
            }
        }

        if (recipientId == null || recipientId.equals(actorId)) return null;

        long exists = notificationService.count(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getRecipientId, recipientId)
                .eq(Notification::getActorId, actorId)
                .eq(Notification::getType, notificationType.getValue())
                .eq(Notification::getTargetType, notificationTargetType.getValue())
                .eq(Notification::getTargetId, targetId));

        if (exists == 0) {
            return notificationService.createNotification(
                    recipientId, actorId,
                    notificationType.getValue(),
                    notificationTargetType.getValue(),
                    targetId,
                    params
            );
        }
        return null;
    }

    public LikeBatchStatusResult batchStatus(LikeBatchStatusRequest request) {
        Long userId = CurrentHold.getCurrentId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.NO_AUTH, PARAM_INVALID);
        }
        if (request.getTargetType() == null || request.getTargetIds() == null || request.getTargetIds().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_EMPTY);
        }

        LikeTargetType targetType = LikeTargetType.getByCode(request.getTargetType());
        if (targetType == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, LIKE_TARGET_TYPE_INVALID);
        }

        List<Long> likedIds = userLikeMapper.findLikedTargetIds(userId, targetType.getCode(), request.getTargetIds());
        Map<String, Boolean> likedMap = new HashMap<>();
        for (Long id : request.getTargetIds()) {
            likedMap.put(String.valueOf(id), likedIds.contains(id));
        }
        return new LikeBatchStatusResult(likedMap);
    }
}