package com.microsoft.aediumbackend.model.dto.article.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArticleBriefDTO {
    private Long id;
    private String title;
    private String subtitle;
    private String coverImage;
    private BigDecimal coverFocusY;
    private Long authorId;
    private Integer likeCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
