package com.microsoft.aediumbackend.model.dto.follow.response;

import lombok.Data;

@Data
public class SocialStats {
    private Long userId;
    private Integer followingCount;
    private Integer followerCount;
}
