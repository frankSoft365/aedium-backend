package com.microsoft.aediumbackend.model.dto.follow;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.FOLLOW_ACTION_INVALID;
import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.FOLLOW_TARGET_USER_ID_INVALID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FollowActionRequest {

    @NotNull(message = FOLLOW_TARGET_USER_ID_INVALID)
    @Positive(message = FOLLOW_TARGET_USER_ID_INVALID)
    private Long targetUserId;

    @NotNull(message = FOLLOW_ACTION_INVALID)
    private Integer action;
}
