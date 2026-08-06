package com.microsoft.aediumbackend.service.impl.comment;

import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.exception.BusinessException;
import com.microsoft.aediumbackend.mapper.CommentMapper;
import com.microsoft.aediumbackend.model.dto.comment.request.CreateCommentRequest;
import com.microsoft.aediumbackend.model.entity.Article;
import com.microsoft.aediumbackend.model.entity.Comment;
import com.microsoft.aediumbackend.model.entity.Notification;
import com.microsoft.aediumbackend.model.enums.CommentStatus;
import com.microsoft.aediumbackend.model.enums.NotificationTargetType;
import com.microsoft.aediumbackend.model.enums.NotificationType;
import com.microsoft.aediumbackend.service.ArticleService;
import com.microsoft.aediumbackend.service.CommentService;
import com.microsoft.aediumbackend.service.NotificationService;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.*;
import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.COMMENT_REPLY_UNABLE;
import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.REPLY_TARGET_ARTIClE_NOT_EQUAL;

@Service
public class CommentPersistService {

    @Resource
    private ArticleService articleService;
    @Resource
    private CommentService commentService;
    @Resource
    private CommentMapper commentMapper;
    @Resource
    private NotificationService notificationService;

    @Transactional(rollbackFor = Exception.class)
    public AddCommentRes persistComment(Long articleId, Long userId, CreateCommentRequest req) {
        Article article = articleService.getById(articleId);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, ARTICLE_NOT_FOUND);
        }
        Long parentId = req.getParentId();

        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setContent(req.getContent());
        // rootId
        // parenId
        // replyToUserId
        comment.setLikeCount(0);
        comment.setReplyCount(0);
        comment.setStatus(CommentStatus.NORMAL.getValue());

        Long parentReplyToUserId = null;

        if (parentId == null) {
            comment.setRootId(null);
            comment.setParentId(null);
            comment.setReplyToUserId(null);
        } else {
            Comment parentComment = commentService.getById(parentId);
            if (parentComment == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, COMMENT_NOT_FOUND);
            }
            CommentStatus status = CommentStatus.getEnumByValue(parentComment.getStatus());
            if (CommentStatus.HIDDEN.equals(status)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, COMMENT_IS_HIDDEN);
            }
            if (CommentStatus.DELETED.equals(status)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, COMMENT_REPLY_UNABLE);
            }
            if (!Objects.equals(parentComment.getArticleId(), articleId)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, REPLY_TARGET_ARTIClE_NOT_EQUAL);
            }
            Long parentCommentRootId = parentComment.getRootId();
            comment.setRootId(parentCommentRootId == null ? parentComment.getId() : parentCommentRootId);
            comment.setParentId(parentComment.getId());
            comment.setReplyToUserId(parentComment.getUserId());
            parentReplyToUserId = parentComment.getReplyToUserId();
        }

        commentService.save(comment);

        boolean isReply = comment.getRootId() != null;

        // 如果是回复评论,增加根评论的 reply_count
        if (isReply) {
            commentMapper.incrementReplyCount(comment.getRootId());
        }

        // 持久化一条通知 notification
        Map<String, Object> params = new HashMap<>();
        Notification notification;
        // 作为根评论
        params.put("articleId", articleId);
        if (!isReply) {
            notification = notificationService.createNotification(
                    article.getAuthorId(),
                    userId,
                    NotificationType.NEW_COMMENT.getValue(),
                    NotificationTargetType.ARTICLE.getValue(),
                    comment.getId(),
                    params
            );
        } else {
            // 作为回复
            params.put("parentId", comment.getParentId());
            params.put("parentReplyToUserId", parentReplyToUserId);
            params.put("rootId", comment.getRootId());
            notification = notificationService.createNotification(
                    comment.getReplyToUserId(),
                    userId,
                    NotificationType.NEW_REPLY.getValue(),
                    NotificationTargetType.COMMENT.getValue(),
                    comment.getId(),
                    params
            );
        }

        return new AddCommentRes(comment, notification);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AddCommentRes {
        private Comment comment;
        private Notification notification;
    }
}
