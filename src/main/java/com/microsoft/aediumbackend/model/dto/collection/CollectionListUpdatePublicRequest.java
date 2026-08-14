package com.microsoft.aediumbackend.model.dto.collection;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 切换收藏列表公开/私有状态请求
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollectionListUpdatePublicRequest {

    @NotNull(message = "列表 ID 不能为空")
    private Long listId;
}
