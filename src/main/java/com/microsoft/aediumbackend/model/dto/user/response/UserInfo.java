package com.microsoft.aediumbackend.model.dto.user.response;

import com.microsoft.aediumbackend.model.dto.follow.response.SocialStats;
import lombok.Data;

@Data
public class UserInfo {
    private Long id;
    private String username;
    private SocialStats socialStats;
    private Membership membership;
    private String bio;
    private String avatar;
    private String about;
}
