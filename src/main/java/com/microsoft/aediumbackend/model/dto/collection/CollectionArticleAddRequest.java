package com.microsoft.aediumbackend.model.dto.collection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 收藏文章请求
 * listId 为空 → 加到默认列表（若无则创建）；不为空 → 加到指定列表
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollectionArticleAddRequest {

    private Long articleId;

    /**
     * 可选。为空表示加到默认列表；不为空表示加到指定列表
     */
    private Long listId;
}
