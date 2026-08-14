package com.microsoft.aediumbackend.model.dto.collection;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建自定义收藏列表请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollectionListCreateRequest {

    @NotBlank(message = "列表名称不能为空")
    @Size(max = 60, message = "列表名称不能超过60个字符")
    private String name;

    @Size(max = 280, message = "列表描述不能超过280个字符")
    private String description;

    /**
     * 是否公开：0-私有 1-公开
     */
    private Integer isPublic;
}
