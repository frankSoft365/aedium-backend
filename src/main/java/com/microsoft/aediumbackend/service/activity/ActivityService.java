package com.microsoft.aediumbackend.service.activity;

import com.microsoft.aediumbackend.commen.CursorPage;
import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.exception.BusinessException;
import com.microsoft.aediumbackend.mapper.NotificationMapper;
import com.microsoft.aediumbackend.model.dto.activity.request.UserActivitiesQuery;
import com.microsoft.aediumbackend.model.dto.activity.response.UserActivity;
import com.microsoft.aediumbackend.model.dto.article.response.ArticleBriefDTO;
import com.microsoft.aediumbackend.model.dto.comment.response.CommentBriefDTO;
import com.microsoft.aediumbackend.model.dto.user.response.UserBriefDTO;
import com.microsoft.aediumbackend.model.entity.Notification;
import com.microsoft.aediumbackend.model.enums.ActivityType;
import com.microsoft.aediumbackend.model.enums.NotificationType;
import com.microsoft.aediumbackend.service.ArticleService;
import com.microsoft.aediumbackend.service.CommentService;
import com.microsoft.aediumbackend.service.UserService;
import com.microsoft.aediumbackend.service.impl.comment.CommentCountService;
import com.microsoft.aediumbackend.utils.CursorPageUtils;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.*;

@Service
public class ActivityService {

    private final List<String> ALL_ACTIVITY_TYPE = List.of(
            NotificationType.LIKE_ARTICLE.getValue(),
            NotificationType.NEW_COMMENT.getValue(),
            NotificationType.NEW_REPLY.getValue()
    );

    private final List<String> CLAPS_ACTIVITY_TYPE = List.of(
            NotificationType.LIKE_ARTICLE.getValue()
    );

    private final List<String> RESPONSES_ACTIVITY_TYPE = List.of(
            NotificationType.NEW_COMMENT.getValue(),
            NotificationType.NEW_REPLY.getValue()
    );

    private final NotificationMapper notificationMapper;
    private final UserService userService;
    private final ArticleService articleService;
    private final CommentCountService commentCountService;
    private final CommentService commentService;

    public ActivityService(NotificationMapper notificationMapper, UserService userService, ArticleService articleService, CommentCountService commentCountService, CommentService commentService) {
        this.notificationMapper = notificationMapper;
        this.userService = userService;
        this.articleService = articleService;
        this.commentCountService = commentCountService;
        this.commentService = commentService;
    }

    public CursorPage<UserActivity> getUserActivityList(UserActivitiesQuery query) {
        Long userId = query.getUserId();

        List<String> activityTypes = query.getActivityTypes();
        List<String> notificationType;
        if (activityTypes.size() == 2) {
            for (String type : activityTypes) {
                ActivityType enumByValue = ActivityType.getEnumByValue(type);
                if (enumByValue == null) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, ACTIVITY_TYPE_INVALID);
                }
            }
            notificationType = ALL_ACTIVITY_TYPE;
        } else if (activityTypes.size() == 1) {
            ActivityType enumByValue = ActivityType.getEnumByValue(activityTypes.get(0));
            if (enumByValue == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, ACTIVITY_TYPE_INVALID);
            }
            notificationType = ActivityType.POST_CLAPPED.equals(enumByValue) ? CLAPS_ACTIVITY_TYPE : RESPONSES_ACTIVITY_TYPE;
        } else {
            throw new BusinessException(ErrorCode.PARAM_ERROR, ACTIVITY_TYPE_INVALID);
        }

        List<Notification> allByActorIdNotifications = notificationMapper.findAllByActorIdNotifications(
                userId,
                query.getLastCreatedAt(),
                query.getLastId(),
                query.getSize() + 1,
                notificationType
        );

        if (allByActorIdNotifications.isEmpty()) {
            return new CursorPage<>(Collections.emptyList(), false, null, null);
        }

        CursorPageUtils.CursorInfo cursorInfo = CursorPageUtils.extract(
                allByActorIdNotifications, query.getSize(), Notification::getCreateTime, Notification::getId);

        Set<Long> userIds = new HashSet<>();
        Set<Long> articleIds = new HashSet<>();
        Set<Long> commentIds = new HashSet<>();

        allByActorIdNotifications.forEach(notification -> {
            if (notification.getType().equals(NotificationType.LIKE_ARTICLE.getValue())) {
                userIds.add(notification.getRecipientId());
                articleIds.add(notification.getTargetId());
            } else if (notification.getType().equals(NotificationType.NEW_COMMENT.getValue())) {
                userIds.add(notification.getRecipientId());
                userIds.add(notification.getActorId());
                Map<String, Object> params = notification.getParams();
                Long articleId = (Long) params.get("articleId");
                articleIds.add(articleId);
                commentIds.add(notification.getTargetId());
            } else if (notification.getType().equals(NotificationType.NEW_REPLY.getValue())) {
                Map<String, Object> params = notification.getParams();
                Long articleId = (Long) params.get("articleId");
                Long parentId = (Long) params.get("parentId");
                userIds.add(notification.getActorId());
                userIds.add(notification.getRecipientId());
                articleIds.add(articleId);
                commentIds.add(notification.getTargetId());
                commentIds.add(parentId);
            } else {
                throw new BusinessException(ErrorCode.PARAM_ERROR, NOTIFICATION_TYPE_INVALID);
            }
        });

        Map<Long, UserBriefDTO> usersBriefMap = userIds.isEmpty()
                ? Collections.emptyMap() : userService.getUsersBriefByIds(userIds);
        Map<Long, ArticleBriefDTO> articleBriefMap = articleIds.isEmpty()
                ? Collections.emptyMap() : articleService.getArticleBriefByIds(articleIds);
        Map<Long, Integer> articleIdCommentCountMap = commentCountService.getCommentCountForArticles(new ArrayList<>(articleIds));

        Map<Long, CommentBriefDTO> commentBriefMap = commentIds.isEmpty()
                ? Collections.emptyMap() : commentService.getCommentBriefByIds(commentIds);

        List<UserActivity> list = allByActorIdNotifications.stream().map(notification -> {
            // 第一种 点赞文章
            if (notification.getType().equals(NotificationType.LIKE_ARTICLE.getValue())) {
                return UserActivity.buildForLikeArticle(
                        notification,
                        articleBriefMap.getOrDefault(notification.getTargetId(), new ArticleBriefDTO()),
                        usersBriefMap,
                        articleIdCommentCountMap
                );
            } else if (notification.getType().equals(NotificationType.NEW_COMMENT.getValue())) {
                Map<String, Object> params = notification.getParams();
                Long articleId = (Long) params.get("articleId");
                return UserActivity.buildForNewComment(
                        notification,
                        commentBriefMap.get(notification.getTargetId()),
                        articleBriefMap.getOrDefault(articleId, new ArticleBriefDTO()),
                        usersBriefMap,
                        articleIdCommentCountMap
                );
            } else if (notification.getType().equals(NotificationType.NEW_REPLY.getValue())) {
                Map<String, Object> params = notification.getParams();
                Long articleId = (Long) params.get("articleId");
                Long parentId = (Long) params.get("parentId");

                return UserActivity.buildForNewReply(
                        notification,
                        commentBriefMap,
                        parentId,
                        articleBriefMap.getOrDefault(articleId, new ArticleBriefDTO()),
                        usersBriefMap
                );
            } else {
                throw new BusinessException(ErrorCode.PARAM_ERROR, NOTIFICATION_TYPE_INVALID);
            }
        }).toList();

        return new CursorPage<>(
                list,
                cursorInfo.isHasMore(),
                cursorInfo.getNextCursorCreatedAt(),
                cursorInfo.getNextCursorId()
        );
    }
}
