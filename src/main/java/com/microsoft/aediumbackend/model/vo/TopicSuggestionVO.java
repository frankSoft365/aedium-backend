package com.microsoft.aediumbackend.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicSuggestionVO {

    /**
     * topic名
     */
    private String name;

    /**
     * topic访问量
     */
    private Long articlesCount;

    /**
     * 新：new 旧：existing
     */
    private String status;
}
