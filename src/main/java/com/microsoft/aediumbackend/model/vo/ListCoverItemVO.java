package com.microsoft.aediumbackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 收藏列表封面预览项（含文章删除标识，便于前端提示用户文章已删除）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListCoverItemVO {

    private Long articleId;

    private String coverImage;

    /**
     * 该文章是否已删除：0-正常 1-已删除
     */
    private Integer isDelete;
}
