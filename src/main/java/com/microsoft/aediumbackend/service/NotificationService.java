package com.microsoft.aediumbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.microsoft.aediumbackend.commen.CursorPage;
import com.microsoft.aediumbackend.commen.CursorPageRequest;
import com.microsoft.aediumbackend.model.dto.notification.response.NotificationCursorPage;
import com.microsoft.aediumbackend.model.dto.notification.response.NotificationVO;
import com.microsoft.aediumbackend.model.dto.notification.response.ReplyNotificationVO;
import com.microsoft.aediumbackend.model.dto.notification.response.UnreadCountVO;
import com.microsoft.aediumbackend.model.entity.Notification;
import com.microsoft.aediumbackend.model.enums.NotificationQueryType;

import java.util.List;
import java.util.Map;

public interface NotificationService extends IService<Notification> {

    /**
     * 创建通知
     */
    Notification createNotification(Long recipientId, Long actorId, String type,
                           String targetType, Long targetId, Map<String, Object> params);


    <T extends NotificationVO> NotificationCursorPage<T> getNotificationsByType(Long userId, CursorPageRequest req, NotificationQueryType queryType, Long watermark);

    /**
     * 根据 notification 获取视图 工具方法
     */
    ReplyNotificationVO toReplyNotificationVO(Notification notification);

    /**
     * 获取未读通知数量
     */
    UnreadCountVO getUnreadCount(Long userId);

    /**
     * 根据通知类型获取未读通知数量
     */
    Long getUnreadCountByType(Long userId, String notificationGroup);

    /**
     * 标记指定分组为已读(更新watermark)
     */
    void markGroupAsRead(Long userId, String notificationGroup, Long maxNotificationId);
}
