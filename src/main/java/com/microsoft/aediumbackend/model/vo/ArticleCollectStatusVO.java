package com.microsoft.aediumbackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 某篇文章在用户各收藏列表中的收藏状态项
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArticleCollectStatusVO {

    private Long listId;

    private String listName;

    /**
     * 是否默认列表：0-自定义 1-默认
     */
    private Integer isDefault;

    /**
     * 是否公开：0-私有 1-公开
     */
    private Integer isPublic;

    /**
     * 该文章是否已收藏到该列表
     */
    private Boolean isCollected;
}
