package com.microsoft.aediumbackend.model.dto.article.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArticleBriefDTO {
    private Long id;
    private String title;
}
