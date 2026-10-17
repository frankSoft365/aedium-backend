package com.microsoft.aediumbackend.model.dto.article.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ArticleWithAuthor {
    private Long id;

    private String title;
    private String subtitle;
    private String coverImage;
    private BigDecimal coverFocusY;

    private String content;

    private Long authorId;
    private String authorName;
    private String authorAvatar;

    private Integer likeCount;

    private LocalDateTime publishTime;

    private Long pinnedByCreatorAt;
}
