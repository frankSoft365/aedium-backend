package com.microsoft.aediumbackend.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArticleVO {
    private Long id;
    private String title;
    private String subtitle;
    private String coverImage;
    private String content;

    private List<TopicInArticleVO> topics;

    private Integer responseNum;

    private Long authorId;
    private String authorAvatar;
    private String authorName;
    private LocalDateTime publishTime;
}
