package com.microsoft.aediumbackend.model.dto.article.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArticleResult {
    // article id
    private Long id;

    /**
     * repost count query
     */
    private Integer repostCount;

    /**
     * viewerEdge
     */
    private ArticleViewerEdge viewerEdge;
}
