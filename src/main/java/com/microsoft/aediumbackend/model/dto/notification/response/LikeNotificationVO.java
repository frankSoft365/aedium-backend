package com.microsoft.aediumbackend.model.dto.notification.response;

import com.microsoft.aediumbackend.model.dto.article.response.ArticleBriefDTO;
import com.microsoft.aediumbackend.model.dto.comment.response.CommentBriefDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LikeNotificationVO extends NotificationVO {
    /**
     * 文章 brief（标题等），LIKE_ARTICLE 和 LIKE_COMMENT 都有值
     */
    private ArticleBriefDTO article;

    /**
     * 评论 brief（仅 LIKE_COMMENT 有值，LIKE_ARTICLE 为 null）
     */
    private CommentBriefDTO comment;
}
