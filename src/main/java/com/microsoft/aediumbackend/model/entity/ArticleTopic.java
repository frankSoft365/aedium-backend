package com.microsoft.aediumbackend.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("`article_topic`")
public class ArticleTopic {
    private Long articleId;
    private Long topicId;
    private LocalDateTime createTime;

    public ArticleTopic(Long articleId, Long topicId) {
        this.articleId = articleId;
        this.topicId = topicId;
    }
}

