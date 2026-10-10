package com.microsoft.aediumbackend.model.dto.repost.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RepostState {
    private Long userId;
    private Long articleId;
    private Boolean hasReposted;
    private String note;
}
