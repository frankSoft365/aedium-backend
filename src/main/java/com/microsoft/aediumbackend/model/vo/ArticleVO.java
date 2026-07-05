package com.microsoft.aediumbackend.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleVO {
    private Long id;
    private String title;
    private String subtitle;
    private String coverImage;
    private String content;

    private Long authorId;
    private String authorAvatar;
    private String authorName;
    private LocalDateTime publishTime;
}
