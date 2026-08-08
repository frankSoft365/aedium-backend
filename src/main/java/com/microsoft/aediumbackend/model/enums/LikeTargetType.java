package com.microsoft.aediumbackend.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LikeTargetType {
    ARTICLE(1, "ARTICLE", "文章"),
    COMMENT(2, "COMMENT", "评论");

    private final int code;
    private final String label;
    private final String description;

    public static LikeTargetType getByCode(Integer code) {
        if (code == null) return null;
        for (LikeTargetType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}