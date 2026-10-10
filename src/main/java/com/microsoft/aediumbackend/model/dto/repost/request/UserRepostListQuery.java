package com.microsoft.aediumbackend.model.dto.repost.request;

import com.microsoft.aediumbackend.commen.CursorPageRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRepostListQuery extends CursorPageRequest {
    @NotNull(message = "用户id不能为空")
    @Min(value = 1, message = "用户id必须大于0")
    private Long userId;
}
