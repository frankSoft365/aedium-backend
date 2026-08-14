package com.microsoft.aediumbackend.model.dto.collection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 列表封面预览项（listId -> 单篇文章封面），用于在 Service 层聚合为每个列表的前 N 个封面。
 * 含文章删除标识：即使文章被软删除也查出，以便前端告知用户。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListCoverDTO {

    private Long listId;

    private Long articleId;

    private String coverImage;

    /**
     * 该文章是否已删除：0-正常 1-已删除
     */
    private Integer isDelete;
}
