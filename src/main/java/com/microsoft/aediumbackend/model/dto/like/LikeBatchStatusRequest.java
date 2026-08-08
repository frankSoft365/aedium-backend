package com.microsoft.aediumbackend.model.dto.like;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LikeBatchStatusRequest {
    private Integer targetType;
    private List<Long> targetIds;
}