package com.microsoft.aediumbackend.service.impl;

import com.microsoft.aediumbackend.commen.CursorPageRequest;
import com.microsoft.aediumbackend.mapper.NotificationMapper;
import com.microsoft.aediumbackend.mapper.NotificationReadStateMapper;
import com.microsoft.aediumbackend.model.dto.notification.response.FollowNotificationVO;
import com.microsoft.aediumbackend.model.dto.notification.response.NotificationCursorPage;
import com.microsoft.aediumbackend.model.dto.user.response.UserBriefDTO;
import com.microsoft.aediumbackend.model.entity.Notification;
import com.microsoft.aediumbackend.model.enums.NotificationQueryType;
import com.microsoft.aediumbackend.model.enums.NotificationTargetType;
import com.microsoft.aediumbackend.model.enums.NotificationType;
import com.microsoft.aediumbackend.service.ArticleService;
import com.microsoft.aediumbackend.service.CommentService;
import com.microsoft.aediumbackend.service.UserService;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("通知服务测试")
class NotificationServiceImplTest {

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private NotificationReadStateMapper notificationReadStateMapper;

    @Mock
    private UserService userService;

    @Mock
    private ArticleService articleService;

    @Mock
    private CommentService commentService;

    @Test
    @DisplayName("关注通知聚合通知基础字段和关注者信息")
    void getNotificationsByType_follow_aggregatesActor() {
        Long recipientId = 1L;
        Long actorId = 2L;
        LocalDateTime createTime = LocalDateTime.of(2026, 8, 24, 12, 0);

        Notification notification = new Notification();
        notification.setId(100L);
        notification.setRecipientId(recipientId);
        notification.setActorId(actorId);
        notification.setType(NotificationType.NEW_FOLLOWER.getValue());
        notification.setTargetType(NotificationTargetType.USER.getValue());
        notification.setTargetId(actorId);
        notification.setCreateTime(createTime);

        UserBriefDTO actor = new UserBriefDTO();
        actor.setId(actorId);
        actor.setUsername("follower");
        actor.setImage("avatar.jpg");

        when(notificationReadStateMapper.getLastReadId(
                recipientId, NotificationQueryType.FOLLOW.getValue())).thenReturn(50L);
        when(notificationMapper.findNotificationsByTypes(
                eq(recipientId),
                isNull(),
                isNull(),
                eq(11),
                eq(NotificationQueryType.FOLLOW.getNotificationTypes())
        )).thenReturn(new ArrayList<>(List.of(notification)));
        when(userService.getUsersBriefByIds(Set.of(actorId))).thenReturn(Map.of(actorId, actor));

        NotificationCursorPage<FollowNotificationVO> result =
                notificationService.getNotificationsByType(
                        recipientId,
                        new CursorPageRequest(null, null, 10),
                        NotificationQueryType.FOLLOW,
                        null
                );

        assertEquals(1, result.getItems().size());
        assertFalse(result.isHasMore());
        assertEquals(50L, result.getWatermark());
        assertEquals(createTime, result.getNextCursorCreatedAt());
        assertEquals(100L, result.getNextCursorId());

        FollowNotificationVO item = result.getItems().get(0);
        assertInstanceOf(FollowNotificationVO.class, item);
        assertEquals(100L, item.getId());
        assertEquals(recipientId, item.getRecipientId());
        assertEquals(actorId, item.getActorId());
        assertEquals("follower", item.getActorUsername());
        assertEquals("avatar.jpg", item.getActorAvatar());
        assertEquals(NotificationType.NEW_FOLLOWER.getValue(), item.getType());
        assertEquals(NotificationTargetType.USER.getValue(), item.getTargetType());
        assertEquals(actorId, item.getTargetId());
        assertEquals(0, item.getIsNew());
        assertEquals(createTime, item.getCreateTime());

        verify(userService).getUsersBriefByIds(Set.of(actorId));
        verifyNoInteractions(articleService, commentService);
    }
}
