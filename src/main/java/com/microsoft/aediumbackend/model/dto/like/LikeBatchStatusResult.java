package com.microsoft.aediumbackend.model.dto.like;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LikeBatchStatusResult {
    private Map<String, Boolean> likedMap;
}