package com.microsoft.aediumbackend.model.dto.follow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FollowBatchStatusResult {

    private Map<Long, Boolean> followingMap;
}
