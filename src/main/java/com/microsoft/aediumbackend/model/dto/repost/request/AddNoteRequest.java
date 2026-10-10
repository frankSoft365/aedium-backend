package com.microsoft.aediumbackend.model.dto.repost.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddNoteRequest {
    @NotNull(message = "文章id不能为空")
    @Min(value = 1, message = "文章id必须大于0")
    private Long articleId;

    @NotBlank(message = "转发note不能为空，不能仅包含空格")
    @Size(max = 280, message = "转发的note不能超过280个字符")
    private String note;
}
