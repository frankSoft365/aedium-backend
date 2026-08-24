package com.microsoft.aediumbackend.model.dto.follow;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.FOLLOW_TARGET_USER_IDS_EMPTY;
import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.FOLLOW_TARGET_USER_IDS_TOO_MANY;
import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.FOLLOW_TARGET_USER_ID_INVALID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FollowBatchStatusRequest {

    @NotEmpty(message = FOLLOW_TARGET_USER_IDS_EMPTY)
    @Size(max = 100, message = FOLLOW_TARGET_USER_IDS_TOO_MANY)
    private List<
            @NotNull(message = FOLLOW_TARGET_USER_ID_INVALID)
            @Positive(message = FOLLOW_TARGET_USER_ID_INVALID)
            Long> targetUserIds;
}
