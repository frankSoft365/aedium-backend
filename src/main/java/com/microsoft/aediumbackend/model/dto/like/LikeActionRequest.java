package com.microsoft.aediumbackend.model.dto.like;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LikeActionRequest {
    private Integer targetType;
    private Long targetId;
    private Integer action;
}