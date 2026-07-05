package com.microsoft.aediumbackend.model.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
public enum PublishStatusEnum {
    PUBLISHED("published", "立即发布"),
    SCHEDULED("scheduled", "定时发布");

    private final String value;
    private final String name;

    PublishStatusEnum(String value, String name) {
        this.value = value;
        this.name = name;
    }

    public static PublishStatusEnum getEnumByValue(String value) {
        for (PublishStatusEnum publishStatusEnum : PublishStatusEnum.values()) {
            if (publishStatusEnum.value.equals(value)) {
                return publishStatusEnum;
            }
        }
        return null;
    }

    public static PublishStatusEnum getEnumByName(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        for (PublishStatusEnum publishStatusEnum : PublishStatusEnum.values()) {
            if (publishStatusEnum.getName().equals(name)) {
                return publishStatusEnum;
            }
        }
        return null;
    }
}
