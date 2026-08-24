package com.microsoft.aediumbackend.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FollowAction {
    FOLLOW(1),
    UNFOLLOW(2);

    private final int code;

    public static FollowAction getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (FollowAction action : values()) {
            if (action.code == code) {
                return action;
            }
        }
        return null;
    }
}
