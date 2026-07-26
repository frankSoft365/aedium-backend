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
public class ReplyNotificationVO extends NotificationVO {
    private CommentBriefDTO rootComment;
    private CommentBriefDTO parentComment;
    private CommentBriefDTO reply;
    private ArticleBriefDTO article;
}
