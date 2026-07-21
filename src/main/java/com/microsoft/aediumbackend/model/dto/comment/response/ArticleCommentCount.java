package com.microsoft.aediumbackend.model.dto.comment.response;

import lombok.Data;

@Data
public class ArticleCommentCount {
    private Long articleId;
    private Integer commentCount;
}
