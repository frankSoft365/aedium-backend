package com.microsoft.aediumbackend.model.dto.activity.response;

import com.microsoft.aediumbackend.model.dto.article.response.ArticleBriefDTO;
import com.microsoft.aediumbackend.model.dto.comment.response.CommentBriefDTO;
import com.microsoft.aediumbackend.model.dto.user.response.UserBriefDTO;
import com.microsoft.aediumbackend.model.entity.Notification;
import com.microsoft.aediumbackend.model.enums.ActivityType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserActivity {
    private Long activityId;
    private String activityType;
    private LocalDateTime occurredAt;
    private Post post;
    // 当 activityType 为 RESPONSE_CREATED 时不为null，
    // 存储所要查询的这个用户对某篇文章的 评论/其下的评论的回复：Post；
    // 当 activityType 为 POST_CLAPPED 时为null
    private Post responsePost;

    public static UserActivity buildForLikeArticle(
            Notification notification, ArticleBriefDTO article, Map<Long, UserBriefDTO> userMap, Map<Long, Integer> articleResponseCountMap
    ) {
        UserActivity userActivity = new UserActivity();
        userActivity.setActivityId(notification.getId());
        userActivity.setActivityType(ActivityType.POST_CLAPPED.getValue());
        userActivity.setOccurredAt(notification.getCreateTime());
        userActivity.setPost(Post.buildForArticle(article, userMap, articleResponseCountMap));
        userActivity.setResponsePost(null);
        return userActivity;
    }

    public static UserActivity buildForNewComment(
            Notification notification, CommentBriefDTO comment, ArticleBriefDTO article, Map<Long, UserBriefDTO> userMap, Map<Long, Integer> articleResponseCountMap
    ) {
        UserActivity userActivity = new UserActivity();
        userActivity.setActivityId(notification.getId());
        userActivity.setActivityType(ActivityType.RESPONSE_CREATED.getValue());
        userActivity.setOccurredAt(notification.getCreateTime());
        userActivity.setPost(Post.buildForArticle(article, userMap, articleResponseCountMap));
        userActivity.setResponsePost(Post.buildForComment(
                comment,
                userMap.get(notification.getActorId())
        ));
        return userActivity;
    }

    public static UserActivity buildForNewReply(
            Notification notification,
            Map<Long, CommentBriefDTO> commentMap,
            Long parentId,
            ArticleBriefDTO article,
            Map<Long, UserBriefDTO> userMap
    ) {
        UserActivity userActivity = new UserActivity();
        userActivity.setActivityId(notification.getId());
        userActivity.setActivityType(ActivityType.RESPONSE_CREATED.getValue());
        userActivity.setOccurredAt(notification.getCreateTime());
        userActivity.setPost(Post.buildForParentComment(
                commentMap.get(parentId),
                userMap.get(notification.getRecipientId()),
                article
        ));
        userActivity.setResponsePost(Post.buildForComment(
                commentMap.get(notification.getTargetId()),
                userMap.get(notification.getActorId())
        ));
        return userActivity;
    }
}
