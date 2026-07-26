package com.microsoft.aediumbackend.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public enum NotificationQueryType {
    REPLY("reply", Arrays.asList(
            NotificationType.NEW_COMMENT.getValue(),
            NotificationType.NEW_REPLY.getValue()
    )),
    LIKE("like", Arrays.asList(
            NotificationType.LIKE_ARTICLE.getValue(),
            NotificationType.LIKE_COMMENT.getValue()
    )),
    FOLLOW("follow", Arrays.asList(
            NotificationType.NEW_FOLLOWER.getValue()
    ));

    private final String value;
    private final List<String> notificationTypes;

    public static NotificationQueryType getEnumByValue(String value) {
        for (NotificationQueryType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
