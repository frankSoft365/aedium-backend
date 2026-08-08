package com.microsoft.aediumbackend.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LikeAction {
    LIKE(1, "LIKE", "点赞"),
    UNLIKE(2, "UNLIKE", "取消点赞");

    private final int code;
    private final String label;
    private final String description;

    public static LikeAction getByCode(Integer code) {
        if (code == null) return null;
        for (LikeAction action : values()) {
            if (action.code == code) {
                return action;
            }
        }
        return null;
    }
}