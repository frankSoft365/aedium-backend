package com.microsoft.aediumbackend.service;

import com.microsoft.aediumbackend.commen.CursorPage;
import com.microsoft.aediumbackend.commen.CursorPageRequest;
import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.exception.BusinessException;
import com.microsoft.aediumbackend.mapper.UserFollowMapper;
import com.microsoft.aediumbackend.mapper.UserMapper;
import com.microsoft.aediumbackend.model.dto.follow.FollowActionRequest;
import com.microsoft.aediumbackend.model.dto.follow.FollowBatchStatusRequest;
import com.microsoft.aediumbackend.model.dto.follow.FollowBatchStatusResult;
import com.microsoft.aediumbackend.model.entity.User;
import com.microsoft.aediumbackend.model.entity.UserFollow;
import com.microsoft.aediumbackend.model.enums.NotificationTargetType;
import com.microsoft.aediumbackend.model.enums.NotificationType;
import com.microsoft.aediumbackend.model.vo.UserVO;
import com.microsoft.aediumbackend.utils.CurrentHold;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("用户关注服务测试")
class UserFollowServiceTest {

    private static final Long CURRENT_USER_ID = 1L;
    private static final Long TARGET_USER_ID = 2L;

    @InjectMocks
    private UserFollowService userFollowService;

    @Mock
    private UserFollowMapper userFollowMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        CurrentHold.setCurrentId(CURRENT_USER_ID);
    }

    @AfterEach
    void tearDown() {
        CurrentHold.removeId();
    }

    @Test
    @DisplayName("批量查询返回每个目标用户的关注状态并去重")
    void batchStatus_returnsStatusForEveryTarget() {
        FollowBatchStatusRequest request = new FollowBatchStatusRequest(List.of(2L, 3L, 2L, 4L));
        when(userFollowMapper.selectList(any())).thenReturn(List.of(
                new UserFollow(CURRENT_USER_ID, 2L, null),
                new UserFollow(CURRENT_USER_ID, 4L, null)
        ));

        FollowBatchStatusResult result = userFollowService.batchStatus(request);

        assertEquals(Map.of(2L, true, 3L, false, 4L, true), result.getFollowingMap());
        verify(userFollowMapper).selectList(any());
    }

    @Test
    @DisplayName("没有关注关系时全部返回未关注")
    void batchStatus_withoutFollows_returnsFalse() {
        FollowBatchStatusRequest request = new FollowBatchStatusRequest(List.of(2L, 3L));
        when(userFollowMapper.selectList(any())).thenReturn(List.of());

        FollowBatchStatusResult result = userFollowService.batchStatus(request);

        assertEquals(Map.of(2L, false, 3L, false), result.getFollowingMap());
    }

    @Test
    @DisplayName("未登录时拒绝查询")
    void batchStatus_withoutLogin_throws() {
        CurrentHold.removeId();
        FollowBatchStatusRequest request = new FollowBatchStatusRequest(List.of(2L));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userFollowService.batchStatus(request)
        );

        assertEquals(ErrorCode.NO_AUTH.getCode(), exception.getCode());
        verify(userFollowMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("首次关注创建关系、更新计数并创建通知")
    void handleAction_follow_createsRelationAndUpdatesCounts() {
        mockLockedUsers(10);
        when(userFollowMapper.selectCount(any())).thenReturn(0L);
        when(userFollowMapper.insert(any(UserFollow.class))).thenReturn(1);
        when(userMapper.update(isNull(), any())).thenReturn(1);
        when(notificationService.count(any())).thenReturn(0L);

        userFollowService.handleAction(new FollowActionRequest(TARGET_USER_ID, 1));

        verify(userFollowMapper).insert(any(UserFollow.class));
        verify(userMapper, times(2)).update(isNull(), any());
        verify(notificationService).createNotification(
                eq(TARGET_USER_ID),
                eq(CURRENT_USER_ID),
                eq(NotificationType.NEW_FOLLOWER.getValue()),
                eq(NotificationTargetType.USER.getValue()),
                eq(CURRENT_USER_ID),
                isNull()
        );
    }

    @Test
    @DisplayName("重复关注幂等成功且不重复修改数据")
    void handleAction_duplicateFollow_isIdempotent() {
        mockLockedUsers(10);
        when(userFollowMapper.selectCount(any())).thenReturn(1L);

        userFollowService.handleAction(new FollowActionRequest(TARGET_USER_ID, 1));

        verify(userFollowMapper, never()).insert(any(UserFollow.class));
        verify(userMapper, never()).update(any(), any());
        verify(notificationService, never()).count(any());
    }

    @Test
    @DisplayName("关注数达到上限时拒绝新增关注")
    void handleAction_followLimitExceeded_throws() {
        mockLockedUsers(5000);
        when(userFollowMapper.selectCount(any())).thenReturn(0L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userFollowService.handleAction(new FollowActionRequest(TARGET_USER_ID, 1))
        );

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
        verify(userFollowMapper, never()).insert(any(UserFollow.class));
        verify(userMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("取消关注物理删除关系并减少双方计数")
    void handleAction_unfollow_deletesRelationAndUpdatesCounts() {
        mockLockedUsers(10);
        when(userFollowMapper.selectCount(any())).thenReturn(1L);
        when(userFollowMapper.delete(any())).thenReturn(1);
        when(userMapper.update(isNull(), any())).thenReturn(1);

        userFollowService.handleAction(new FollowActionRequest(TARGET_USER_ID, 2));

        verify(userFollowMapper).delete(any());
        verify(userMapper, times(2)).update(isNull(), any());
        verify(notificationService, never()).createNotification(
                any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("重复取消关注幂等成功且不修改计数")
    void handleAction_duplicateUnfollow_isIdempotent() {
        mockLockedUsers(10);
        when(userFollowMapper.selectCount(any())).thenReturn(0L);

        userFollowService.handleAction(new FollowActionRequest(TARGET_USER_ID, 2));

        verify(userFollowMapper, never()).delete(any());
        verify(userMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("不能关注自己")
    void handleAction_followSelf_throws() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userFollowService.handleAction(new FollowActionRequest(CURRENT_USER_ID, 1))
        );

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
        verify(userMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("目标用户不存在时拒绝操作")
    void handleAction_targetNotFound_throws() {
        User currentUser = new User();
        currentUser.setId(CURRENT_USER_ID);
        currentUser.setFollowingCount(10);
        when(userMapper.selectList(any())).thenReturn(List.of(currentUser));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userFollowService.handleAction(new FollowActionRequest(TARGET_USER_ID, 1))
        );

        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
        verify(userFollowMapper, never()).selectCount(any());
    }

    @Test
    @DisplayName("非法操作类型被拒绝")
    void handleAction_invalidAction_throws() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userFollowService.handleAction(new FollowActionRequest(TARGET_USER_ID, 3))
        );

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
        verify(userMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("粉丝列表按关注关系顺序返回UserVO并生成下一页游标")
    void getFollowers_returnsUsersInFollowOrder() {
        LocalDateTime firstTime = LocalDateTime.of(2026, 8, 24, 12, 0);
        LocalDateTime secondTime = firstTime.minusMinutes(1);
        LocalDateTime extraTime = firstTime.minusMinutes(2);
        when(userFollowMapper.selectList(any())).thenReturn(new ArrayList<>(List.of(
                new UserFollow(3L, CURRENT_USER_ID, firstTime),
                new UserFollow(2L, CURRENT_USER_ID, secondTime),
                new UserFollow(4L, CURRENT_USER_ID, extraTime)
        )));
        when(userMapper.selectList(any())).thenReturn(List.of(
                createUser(2L, "user-2"),
                createUser(3L, "user-3")
        ));

        CursorPage<UserVO> result = userFollowService.getFollowers(
                CURRENT_USER_ID,
                new CursorPageRequest(null, null, 2)
        );

        assertEquals(List.of(3L, 2L), result.getItems().stream().map(UserVO::getId).toList());
        assertTrue(result.isHasMore());
        assertEquals(secondTime, result.getNextCursorCreatedAt());
        assertEquals(2L, result.getNextCursorId());
    }

    @Test
    @DisplayName("关注列表返回被关注用户")
    void getFollowing_returnsFollowedUsers() {
        LocalDateTime followTime = LocalDateTime.of(2026, 8, 24, 12, 0);
        when(userFollowMapper.selectList(any())).thenReturn(new ArrayList<>(List.of(
                new UserFollow(CURRENT_USER_ID, TARGET_USER_ID, followTime)
        )));
        when(userMapper.selectList(any())).thenReturn(List.of(createUser(TARGET_USER_ID, "target")));

        CursorPage<UserVO> result = userFollowService.getFollowing(
                CURRENT_USER_ID,
                new CursorPageRequest(null, null, 12)
        );

        assertEquals(1, result.getItems().size());
        assertEquals(TARGET_USER_ID, result.getItems().get(0).getId());
        assertEquals("target", result.getItems().get(0).getUsername());
        assertFalse(result.isHasMore());
        assertEquals(followTime, result.getNextCursorCreatedAt());
        assertEquals(TARGET_USER_ID, result.getNextCursorId());
    }

    @Test
    @DisplayName("没有关注关系时返回空分页且不查询用户")
    void getFollowers_withoutRelations_returnsEmptyPage() {
        when(userFollowMapper.selectList(any())).thenReturn(List.of());

        CursorPage<UserVO> result = userFollowService.getFollowers(
                CURRENT_USER_ID,
                new CursorPageRequest(null, null, 12)
        );

        assertTrue(result.getItems().isEmpty());
        assertFalse(result.isHasMore());
        verify(userMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("游标参数不完整时拒绝查询列表")
    void getFollowers_withIncompleteCursor_throws() {
        CursorPageRequest request = new CursorPageRequest(
                LocalDateTime.of(2026, 8, 24, 12, 0),
                null,
                12
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userFollowService.getFollowers(CURRENT_USER_ID, request)
        );

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
        verify(userFollowMapper, never()).selectList(any());
    }

    private void mockLockedUsers(int followingCount) {
        User currentUser = new User();
        currentUser.setId(CURRENT_USER_ID);
        currentUser.setFollowingCount(followingCount);

        User targetUser = new User();
        targetUser.setId(TARGET_USER_ID);
        targetUser.setFollowerCount(20);

        when(userMapper.selectList(any())).thenReturn(List.of(currentUser, targetUser));
    }

    private User createUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setFollowerCount(0);
        user.setFollowingCount(0);
        return user;
    }
}
