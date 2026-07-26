package com.microsoft.aediumbackend.model.dto.notification.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationVO {
    private Long id;

    /**
     * 接收者用户ID
     */
    private Long recipientId;

    /**
     * 发起者用户ID
     */
    private Long actorId;
    private String actorAvatar;
    private String actorUsername;

    /**
     * 通知类型：NEW_COMMENT, NEW_REPLY, LIKE_ARTICLE, LIKE_COMMENT, NEW_FOLLOWER
     */
    private String type;

    /**
     * 目标类型：ARTICLE, COMMENT, USER
     */
    private String targetType;

    /**
     * 目标ID(关联的主实体ID)
     */
    private Long targetId;

    private Integer isNew;

    private LocalDateTime createTime;
}
