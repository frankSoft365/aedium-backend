package com.microsoft.aediumbackend.model.dto.user.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserProfileQuery {
    @NotNull(message = "用户id不能为空")
    @Min(value = 1, message = "用户id必须大于0")
    private Long userId;
}
