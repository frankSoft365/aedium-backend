package com.microsoft.aediumbackend.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRepost {
    private Long userId;
    private Long articleId;
    private String note;
    private LocalDateTime createTime;
}
