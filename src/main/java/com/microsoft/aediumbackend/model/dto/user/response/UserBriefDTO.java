package com.microsoft.aediumbackend.model.dto.user.response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserBriefDTO {
    private Long id;
    private String username;
    private String image;
}
