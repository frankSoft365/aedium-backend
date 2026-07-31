package com.microsoft.aediumbackend.model.dto.notificationPush.request;

import com.microsoft.aediumbackend.model.dto.notification.response.NotificationVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPushRequest<T extends NotificationVO> {
    private Long recipientId;
    private String type;
    private Long unreadCount;
    private T notificationVO;
}
