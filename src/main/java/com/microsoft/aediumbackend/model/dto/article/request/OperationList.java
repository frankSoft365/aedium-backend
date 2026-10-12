package com.microsoft.aediumbackend.model.dto.article.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OperationList {
    @Valid
    @NotNull(message = "operationList不能为null")
    private List<Operation> operationList;
}
