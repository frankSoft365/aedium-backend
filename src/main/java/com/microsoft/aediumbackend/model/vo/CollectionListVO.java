package com.microsoft.aediumbackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 收藏列表展示项（个人主页 Lists 页）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollectionListVO {

    private Long id;

    private String name;

    private String description;

    /**
     * 列表内文章数量（冗余计数）
     */
    private Integer articleCount;

    /**
     * 是否公开：0-私有 1-公开
     */
    private Integer isPublic;

    /**
     * 是否默认列表：0-自定义 1-默认
     */
    private Integer isDefault;

    /**
     * 前几篇文章的封面（预览，含已删除文章，前端可据 isDelete 提示用户）
     */
    private List<ListCoverItemVO> coverImages;
}
