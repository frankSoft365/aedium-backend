package com.microsoft.aediumbackend.model.dto.article;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ArticleUpdateRequest {
    @NotNull(message = "articleId不能为空")
    private Long articleId;
    
    @NotBlank(message = "content不能为空")
    private String content;
}
