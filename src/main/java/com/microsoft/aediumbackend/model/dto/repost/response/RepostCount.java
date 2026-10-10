package com.microsoft.aediumbackend.model.dto.repost.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RepostCount {
    private Long id;
    private Integer repostCount;
}
