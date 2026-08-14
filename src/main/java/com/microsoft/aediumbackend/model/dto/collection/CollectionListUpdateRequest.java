package com.microsoft.aediumbackend.model.dto.collection;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新收藏列表请求（部分字段更新，null 字段不更新）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollectionListUpdateRequest {

    @NotNull(message = "列表 ID 不能为空")
    private Long listId;

    /**
     * 可选。如果传值则不能为空白，最长 60 字符
     */
    @Size(max = 60, message = "列表名称不能超过60个字符")
    private String name;

    /**
     * 可选。最长 280 字符
     */
    @Size(max = 280, message = "列表描述不能超过280个字符")
    private String description;

    /**
     * 可选。是否公开：0-私有 1-公开
     */
    private Integer isPublic;
}
