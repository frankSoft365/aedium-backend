package com.microsoft.aediumbackend.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommentStatus {
    NORMAL("NORMAL", "正常"),
    DELETED("DELETED", "已删除"),
    HIDDEN("HIDDEN", "隐藏");

    private final String value;
    private final String description;

    public static CommentStatus getEnumByValue(String value) {
        for (CommentStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return null;
    }
}
