package com.microsoft.aediumbackend.model.dto.article.request;

import com.microsoft.aediumbackend.commen.CursorPageRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserArticleListRequest extends CursorPageRequest {
    @NotNull(message = "用户id不能为空")
    @Min(value = 1, message = "用户id必须大于0")
    private Long userId;
}
