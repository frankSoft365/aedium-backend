package com.microsoft.aediumbackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 收藏列表基础信息（不含封面预览，用于根据 id 查询单个列表信息）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollectionListInfoVO {

    private Long id;

    private Long userId;

    private String name;

    private String description;

    /**
     * 是否公开：0-私有 1-公开
     */
    private Integer isPublic;

    /**
     * 是否默认列表：0-自定义 1-默认
     */
    private Integer isDefault;

    /**
     * 列表内文章数量（冗余计数）
     */
    private Integer articleCount;
}
