package com.microsoft.aediumbackend.model.dto.article.request;

import com.microsoft.aediumbackend.model.enums.OperationNameEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用于请求文章结果的参数：操作
 */
@Data
public class Operation {
    // 操作名 "RepostCountQuery" "CollectionBaseQuery"
    @NotNull(message = "operationName不能为空")
    private OperationNameEnum operationName;
    // 请求的id
    @NotNull(message = "id不能为空")
    @Min(value = 1, message = "id必须大于0")
    private Long id;
}

