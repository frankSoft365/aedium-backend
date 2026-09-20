package com.microsoft.aediumbackend.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ActivityType {
    POST_CLAPPED("POST_CLAPPED", "对文章的点赞"),
    RESPONSE_CREATED("RESPONSE_CREATED", "评论的创建")
    ;

    private final String value;
    private final String description;

    public static ActivityType getEnumByValue(String value) {
        for (ActivityType activityType : values()) {
            if (activityType.value.equals(value)) {
                return activityType;
            }
        }
        return null;
    }

}
