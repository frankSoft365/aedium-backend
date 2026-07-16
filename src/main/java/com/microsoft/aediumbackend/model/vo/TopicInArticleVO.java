package com.microsoft.aediumbackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopicInArticleVO {
    private Long id;
    private String name;
    private String slug;
}
