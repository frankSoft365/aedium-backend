package com.microsoft.aediumbackend.model.dto.collection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 取消收藏（删除某条 list-article 关系）请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollectionArticleRemoveRequest {

    private Long listId;

    private Long articleId;
}
