package com.microsoft.aediumbackend.commen;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 删除请求
 */
@Data
public class DeleteRequest {

    /**
     * id
     */
    @NotNull(message = "删除id不能为空")
    @Min(value = 1, message = "id必须为大于0的正整数")
    private Long id;

}