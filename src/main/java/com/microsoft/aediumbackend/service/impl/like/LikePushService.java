package com.microsoft.aediumbackend.service.impl.like;

import com.microsoft.aediumbackend.model.dto.notification.response.LikeNotificationVO;
import com.microsoft.aediumbackend.model.dto.notificationPush.request.NotificationPushRequest;
import com.microsoft.aediumbackend.model.entity.Notification;
import com.microsoft.aediumbackend.model.enums.NotificationQueryType;
import com.microsoft.aediumbackend.service.NotificationService;
import com.microsoft.aediumbackend.service.impl.notification.NotificationPushService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 点赞通知异步推送
 * 借鉴 {@link com.microsoft.aediumbackend.service.impl.comment.CommentPostProcessService} 的结构
 * 在点赞事务提交后由调用方触发，构造完整 VO + 未读数一并推送到接收者的 WebSocket 通道
 */
@Service
@Slf4j
public class LikePushService {

    @Resource
    private NotificationService notificationService;
    @Resource
    private NotificationPushService notificationPushService;

    @Async("notificationPushExecutor")
    public void pushLikeNotification(Notification notification) {
        try {
            LikeNotificationVO vo = notificationService.toLikeNotificationVO(notification);
            Long recipientId = vo.getRecipientId();
            Long unreadLikeCount = notificationService.getUnreadCountByType(
                    recipientId, NotificationQueryType.LIKE.getValue());
            NotificationPushRequest<LikeNotificationVO> request = new NotificationPushRequest<>(
                    recipientId,
                    NotificationQueryType.LIKE.getValue(),
                    unreadLikeCount,
                    vo
            );
            notificationPushService.pushUnreadCount(request);
        } catch (Exception e) {
            // MUST catch here — @Async void 方法的异常只会被 Spring 默认处理器记录，
            // 不会传播给调用方，且此时调用方早已返回响应
            log.error("点赞消息推送失败: notification {}", notification.getId(), e);
        }
    }
}
