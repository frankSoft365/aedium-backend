package com.microsoft.aediumbackend.service.impl.notification;

import com.microsoft.aediumbackend.model.dto.notification.response.NotificationVO;
import com.microsoft.aediumbackend.model.dto.notificationPush.request.NotificationPushRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationPushService {

    private final SimpMessagingTemplate simpMessagingTemplate;

    public NotificationPushService(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public <T extends NotificationVO> void pushUnreadCount(NotificationPushRequest<T> request) {
        simpMessagingTemplate.convertAndSendToUser(
                request.getRecipientId().toString(),
                "/queue/notifications",
                request
        );
    }
}
