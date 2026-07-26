package com.microsoft.aediumbackend.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationTargetType {
    ARTICLE("ARTICLE", "文章"),
    COMMENT("COMMENT", "评论"),
    USER("USER", "用户");

    private final String value;
    private final String description;

    public static NotificationTargetType getEnumByValue(String value) {
        for (NotificationTargetType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
