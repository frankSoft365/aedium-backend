package com.microsoft.aediumbackend.model.dto.repost.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RemoveNoteRequest {
    @NotNull(message = "文章id不能为空")
    @Min(value = 1, message = "文章id必须大于0")
    private Long articleId;
}
