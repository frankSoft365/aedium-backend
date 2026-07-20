package com.microsoft.aediumbackend.model.dto.article;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ArticleUpdateRequest {
    @NotNull(message = "articleId不能为空")
    private Long articleId;
    
    @NotBlank(message = "content不能为空")
    private String content;

    @NotBlank(message = "title不能为空")
    private String title;

    private String subtitle;

    private String coverImage;

    private BigDecimal coverFocusY;
}

