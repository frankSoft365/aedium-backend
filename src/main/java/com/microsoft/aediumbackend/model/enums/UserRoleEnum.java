package com.microsoft.aediumbackend.model.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
public enum UserRoleEnum {
    ADMIN_ROLE("admin", "管理员"),
    DEFAULT_ROLE("user", "普通用户"),
    ;
    private final String value;
    private final String name;

    UserRoleEnum(String value, String name) {
        this.value = value;
        this.name = name;
    }

    public static UserRoleEnum getEnumByValue(String value) {
        for (UserRoleEnum userRoleEnum : UserRoleEnum.values()) {
            if (userRoleEnum.value.equals(value)) {
                return userRoleEnum;
            }
        }
        return null;
    }

    public static UserRoleEnum getEnumByName(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        for (UserRoleEnum userRoleEnum : UserRoleEnum.values()) {
            if (userRoleEnum.getName().equals(name)) {
                return userRoleEnum;
            }
        }
        return null;
    }
}
