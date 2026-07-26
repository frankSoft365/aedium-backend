package com.microsoft.aediumbackend.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationType {
    NEW_COMMENT("NEW_COMMENT", "新评论"),
    NEW_REPLY("NEW_REPLY", "新回复"),
    LIKE_ARTICLE("LIKE_ARTICLE", "文章点赞"),
    LIKE_COMMENT("LIKE_COMMENT", "评论点赞"),
    NEW_FOLLOWER("NEW_FOLLOWER", "新关注");

    private final String value;
    private final String description;

    public static NotificationType getEnumByValue(String value) {
        for (NotificationType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
