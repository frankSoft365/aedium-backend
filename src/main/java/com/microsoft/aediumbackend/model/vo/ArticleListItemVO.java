package com.microsoft.aediumbackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArticleListItemVO {
    private Long id;
    private Long authorId;
    private String authorName;
    private String authorAvatar;
    private LocalDateTime publishTime;
    private String title;
    private String subtitle;
    private String coverImage;
}
