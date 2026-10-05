package com.microsoft.aediumbackend.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleVO {
    private Long id;
    private String title;
    private String subtitle;
    private String coverImage;
    private BigDecimal coverFocusY;
    private String content;

    private List<TopicInArticleVO> topics;

    private Integer responseNum;
    private Integer likeCount;

    private Long authorId;
    private String authorAvatar;
    private String authorName;
    private LocalDateTime publishTime;
}
