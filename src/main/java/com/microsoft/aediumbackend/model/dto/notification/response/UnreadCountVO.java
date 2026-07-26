package com.microsoft.aediumbackend.model.dto.notification.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountVO {
    private Long replyCount;
    private Long likeCount;
    private Long followCount;
}
