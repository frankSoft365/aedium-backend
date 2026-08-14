package com.microsoft.aediumbackend.model.dto.collection;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量取消收藏请求（从指定列表中移除多篇文章）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollectionArticleBatchRemoveRequest {

    private Long listId;

    @NotEmpty(message = "文章 ID 列表不能为空")
    private List<Long> articleIds;
}
