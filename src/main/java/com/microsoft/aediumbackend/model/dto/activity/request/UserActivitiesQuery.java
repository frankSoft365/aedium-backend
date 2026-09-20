package com.microsoft.aediumbackend.model.dto.activity.request;

import com.microsoft.aediumbackend.commen.CursorPageRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserActivitiesQuery extends CursorPageRequest {
    // 要查询的用户的id
    private Long userId;
    // 查询类型 ["POST_CLAPPED", "RESPONSE_CREATED"]
    // 相关枚举 ActivityType.java
    private List<String> activityTypes;
}
