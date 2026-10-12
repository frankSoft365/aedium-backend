package com.microsoft.aediumbackend.model.dto.article.response;

import com.microsoft.aediumbackend.model.dto.collection.response.CollectionsConnection;
import lombok.Data;

@Data
public class ArticleViewerEdge {
    /**
     * viewerEdge id
     * 例子：articleId:55fb9cf960d9-viewerId:3d34188467c3
     * articleId 用户观察的文章id
     * viewerId 作为观察者的用户id
     */
    private String id;

    /**
     * 观察收藏夹
     */
    private CollectionsConnection collectionsConnection;
}
