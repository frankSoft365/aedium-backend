package com.microsoft.aediumbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.microsoft.aediumbackend.commen.CursorPageRequest;
import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.exception.BusinessException;
import com.microsoft.aediumbackend.mapper.NotificationMapper;
import com.microsoft.aediumbackend.mapper.NotificationReadStateMapper;
import com.microsoft.aediumbackend.model.dto.article.response.ArticleBriefDTO;
import com.microsoft.aediumbackend.model.dto.comment.response.CommentBriefDTO;
import com.microsoft.aediumbackend.model.dto.notification.response.LikeNotificationVO;
import com.microsoft.aediumbackend.model.dto.notification.response.NotificationCursorPage;
import com.microsoft.aediumbackend.model.dto.notification.response.NotificationVO;
import com.microsoft.aediumbackend.model.dto.notification.response.ReplyNotificationVO;
import com.microsoft.aediumbackend.model.dto.notification.response.UnreadCountVO;
import com.microsoft.aediumbackend.model.dto.user.response.UserBriefDTO;
import com.microsoft.aediumbackend.model.entity.Notification;
import com.microsoft.aediumbackend.model.enums.NotificationQueryType;
import com.microsoft.aediumbackend.model.enums.NotificationType;
import com.microsoft.aediumbackend.service.ArticleService;
import com.microsoft.aediumbackend.service.CommentService;
import com.microsoft.aediumbackend.service.NotificationService;
import com.microsoft.aediumbackend.service.UserService;
import com.microsoft.aediumbackend.utils.CursorPageUtils;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.*;

@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    @Resource
    private NotificationMapper notificationMapper;
    @Resource
    private UserService userService;
    @Resource
    private ArticleService articleService;
    @Lazy
    @Resource
    private CommentService commentService;
    @Resource
    private NotificationReadStateMapper notificationReadStateMapper;

    @Override
    public Notification createNotification(Long recipientId, Long actorId, String type,
                                   String targetType, Long targetId, Map<String, Object> params) {
        // 不给自己发通知
        if (recipientId.equals(actorId)) {
            return null;
        }

        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setActorId(actorId);
        notification.setType(type);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setParams(params);

        this.save(notification);
        return notification;
    }

    /**
     * 根据通知类型获取通知列表视图
     * @param userId 接收者id
     * @param req 游标分页查询参数
     * @param queryType 通知类型
     * @param watermark 该次回话的未读水印
     * @return 通知视图列表
     * @param <T> 其一 评论类型通知视图
     */
    @Override
    @Transactional
    public <T extends NotificationVO> NotificationCursorPage<T> getNotificationsByType(Long userId, CursorPageRequest req, NotificationQueryType queryType, Long watermark) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_FORMAT_ERROR);
        }

        Long lastReadId = watermark != null ? watermark : notificationReadStateMapper.getLastReadId(userId, queryType.getValue());

        List<String> notificationTypes = queryType.getNotificationTypes();
        List<Notification> notificationsRaw = notificationMapper.findNotificationsByTypes(
                userId,
                req.getLastCreatedAt(),
                req.getLastId(),
                req.getSize() + 1,
                notificationTypes
        );

        if (notificationsRaw.isEmpty()) {
            return new NotificationCursorPage<>(Collections.emptyList(), null, false, null, null);
        }

        // 修改了 notificationsRaw
        CursorPageUtils.CursorInfo cursorInfo = CursorPageUtils.extract(
                notificationsRaw, req.getSize(), Notification::getCreateTime, Notification::getId);

        List<T> list = null;

        Set<Long> userIds = new HashSet<>();
        Set<Long> articleIds = new HashSet<>();
        Set<Long> commentIds = new HashSet<>();

        if (NotificationQueryType.REPLY.equals(queryType)) {
            notificationsRaw.forEach(notification -> {
                userIds.add(notification.getActorId());

                commentIds.add(notification.getTargetId());
                Map<String, Object> params = notification.getParams();
                Long articleId = (Long) params.get("articleId");
                articleIds.add(articleId);
                if (notification.getType().equals(NotificationType.NEW_REPLY.getValue())) {
                    userIds.add(notification.getRecipientId());
                    Long rootCommentId = (Long) params.get("rootId");
                    Long parentId = (Long) params.get("parentId");
                    Long parentReplyToUserId = (Long) params.get("parentReplyToUserId");
                    if (rootCommentId != null) {
                        commentIds.add(rootCommentId);
                    }
                    if (parentId != null) {
                        commentIds.add(parentId);
                    }
                    if (parentReplyToUserId != null) {
                        userIds.add(parentReplyToUserId);
                    }
                }
            });
        } else if (NotificationQueryType.LIKE.equals(queryType)) {
            notificationsRaw.forEach(notification -> {
                userIds.add(notification.getActorId());
                if (NotificationType.LIKE_ARTICLE.getValue().equals(notification.getType())) {
                    articleIds.add(notification.getTargetId());
                } else {
                    commentIds.add(notification.getTargetId());
                    Map<String, Object> params = notification.getParams();
                    if (params != null) {
                        articleIds.add((Long) params.get("articleId"));
                    }
                }
            });
        }

        Map<Long, UserBriefDTO> usersBriefMap = userService.getUsersBriefByIds(userIds);
        Map<Long, ArticleBriefDTO> articleBriefMap = articleService.getArticleBriefByIds(articleIds);
        Map<Long, CommentBriefDTO> commentBriefMap = commentService.getCommentBriefByIds(commentIds);

        if (NotificationQueryType.REPLY.equals(queryType)) {
            list = (List<T>) notificationsRaw.stream()
                    .map(notification -> toReplyNotificationVO(notification, lastReadId, usersBriefMap, articleBriefMap, commentBriefMap))
                    .toList();
        } else if (NotificationQueryType.LIKE.equals(queryType)) {
            list = (List<T>) notificationsRaw.stream()
                    .map(notification -> toLikeNotificationVO(notification, lastReadId, usersBriefMap, articleBriefMap, commentBriefMap))
                    .toList();
        }
        return new NotificationCursorPage<>(
                list,
                lastReadId,
                cursorInfo.isHasMore(),
                cursorInfo.getNextCursorCreatedAt(),
                cursorInfo.getNextCursorId()
        );
    }

    public ReplyNotificationVO toReplyNotificationVO(Notification notification) {
        Long lastReadId = notificationReadStateMapper.getLastReadId(notification.getRecipientId(), NotificationQueryType.REPLY.getValue());
        Set<Long> userIds = new HashSet<>();
        Set<Long> articleIds = new HashSet<>();
        Set<Long> commentIds = new HashSet<>();
        userIds.add(notification.getActorId());

        commentIds.add(notification.getTargetId());
        Map<String, Object> params = notification.getParams();
        Long articleId = (Long) params.get("articleId");
        articleIds.add(articleId);
        if (notification.getType().equals(NotificationType.NEW_REPLY.getValue())) {
            userIds.add(notification.getRecipientId());
            Long rootCommentId = (Long) params.get("rootId");
            Long parentId = (Long) params.get("parentId");
            Long parentReplyToUserId = (Long) params.get("parentReplyToUserId");
            if (rootCommentId != null) {
                commentIds.add(rootCommentId);
            }
            if (parentId != null) {
                commentIds.add(parentId);
            }
            if (parentReplyToUserId != null) {
                userIds.add(parentReplyToUserId);
            }
        }

        Map<Long, UserBriefDTO> usersBriefMap = userService.getUsersBriefByIds(userIds);
        Map<Long, ArticleBriefDTO> articleBriefMap = articleService.getArticleBriefByIds(articleIds);
        Map<Long, CommentBriefDTO> commentBriefMap = commentService.getCommentBriefByIds(commentIds);

        return toReplyNotificationVO(notification, lastReadId, usersBriefMap, articleBriefMap, commentBriefMap);
    }

    private ReplyNotificationVO toReplyNotificationVO(
            Notification notification,
            Long lastReadId,
            Map<Long, UserBriefDTO> usersBriefMap,
            Map<Long, ArticleBriefDTO> articleBriefMap,
            Map<Long, CommentBriefDTO> commentBriefMap
    ) {
        ReplyNotificationVO replyNotificationVO = new ReplyNotificationVO();

        Long id = notification.getId();
        Long recipientId = notification.getRecipientId();
        Long actorId = notification.getActorId();
        String type = notification.getType();
        String targetType = notification.getTargetType();
        Long targetId = notification.getTargetId();
        Map<String, Object> params = notification.getParams();
        Long articleId = (Long) params.get("articleId");

        replyNotificationVO.setId(id);
        replyNotificationVO.setRecipientId(recipientId);
        replyNotificationVO.setActorId(actorId);
        UserBriefDTO actor = usersBriefMap.get(actorId);
        replyNotificationVO.setActorAvatar(actor.getImage());
        replyNotificationVO.setActorUsername(actor.getUsername());
        replyNotificationVO.setType(type);
        replyNotificationVO.setTargetType(targetType);
        replyNotificationVO.setTargetId(targetId);
        replyNotificationVO.setIsNew(lastReadId != null && id > lastReadId ? 0 : 1);
        replyNotificationVO.setCreateTime(notification.getCreateTime());

        // params
        CommentBriefDTO reply = commentBriefMap.get(targetId);

        // 回复文章
        if (NotificationType.NEW_COMMENT.getValue().equals(type)) {
            replyNotificationVO.setRootComment(null);
            replyNotificationVO.setParentComment(null);
            replyNotificationVO.setReply(reply);
        }
        // 回复评论
        if (NotificationType.NEW_REPLY.getValue().equals(type)) {
            if (!recipientId.equals(reply.getReplyToUserId())) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, NOTIFICATION_INCONSISTENCY);
            }
            reply.setReplyToUsername(usersBriefMap.get(recipientId).getUsername());
            replyNotificationVO.setReply(reply);

            Long parentId = (Long) params.get("parentId");
            CommentBriefDTO parentComment = commentBriefMap.get(parentId);
            if (!recipientId.equals(parentComment.getUserId())) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, NOTIFICATION_INCONSISTENCY);
            }
            parentComment.setUsername(usersBriefMap.get(recipientId).getUsername());
            Long parentReplyToUserId = (Long) params.get("parentReplyToUserId");
            if (parentReplyToUserId != null) {
                parentComment.setReplyToUsername(usersBriefMap.get(parentReplyToUserId).getUsername());
            }
            replyNotificationVO.setParentComment(parentComment);

            Long rootCommentId = (Long) params.get("rootId");
            replyNotificationVO.setRootComment(commentBriefMap.get(rootCommentId));
        }

        replyNotificationVO.setArticle(articleBriefMap.get(articleId));
        return replyNotificationVO;
    }

    @Override
    public LikeNotificationVO toLikeNotificationVO(Notification notification) {
        Long lastReadId = notificationReadStateMapper.getLastReadId(
                notification.getRecipientId(), NotificationQueryType.LIKE.getValue());

        Set<Long> userIds = new HashSet<>();
        Set<Long> articleIds = new HashSet<>();
        Set<Long> commentIds = new HashSet<>();

        userIds.add(notification.getActorId());
        if (NotificationType.LIKE_ARTICLE.getValue().equals(notification.getType())) {
            articleIds.add(notification.getTargetId());
        } else {
            commentIds.add(notification.getTargetId());
            Map<String, Object> params = notification.getParams();
            if (params != null) {
                articleIds.add((Long) params.get("articleId"));
            }
        }

        Map<Long, UserBriefDTO> usersBriefMap = userService.getUsersBriefByIds(userIds);
        Map<Long, ArticleBriefDTO> articleBriefMap = articleService.getArticleBriefByIds(articleIds);
        Map<Long, CommentBriefDTO> commentBriefMap = commentService.getCommentBriefByIds(commentIds);

        return toLikeNotificationVO(notification, lastReadId, usersBriefMap, articleBriefMap, commentBriefMap);
    }

    private LikeNotificationVO toLikeNotificationVO(
            Notification notification,
            Long lastReadId,
            Map<Long, UserBriefDTO> usersBriefMap,
            Map<Long, ArticleBriefDTO> articleBriefMap,
            Map<Long, CommentBriefDTO> commentBriefMap
    ) {
        LikeNotificationVO vo = new LikeNotificationVO();

        Long id = notification.getId();
        Long actorId = notification.getActorId();
        String type = notification.getType();
        String targetType = notification.getTargetType();
        Long targetId = notification.getTargetId();

        vo.setId(id);
        vo.setRecipientId(notification.getRecipientId());
        vo.setActorId(actorId);
        UserBriefDTO actor = usersBriefMap.get(actorId);
        vo.setActorAvatar(actor.getImage());
        vo.setActorUsername(actor.getUsername());
        vo.setType(type);
        vo.setTargetType(targetType);
        vo.setTargetId(targetId);
        vo.setIsNew(lastReadId != null && id > lastReadId ? 0 : 1);
        vo.setCreateTime(notification.getCreateTime());

        if (NotificationType.LIKE_ARTICLE.getValue().equals(type)) {
            vo.setArticle(articleBriefMap.get(targetId));
            vo.setComment(null);
        } else {
            vo.setComment(commentBriefMap.get(targetId));
            Map<String, Object> params = notification.getParams();
            if (params != null) {
                vo.setArticle(articleBriefMap.get((Long) params.get("articleId")));
            }
        }

        return vo;
    }

    @Override
    public UnreadCountVO getUnreadCount(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "参数不能为空");
        }

        long replyCount = countUnreadByGroup(userId, NotificationQueryType.REPLY);
        long likeCount = countUnreadByGroup(userId, NotificationQueryType.LIKE);
        long followCount = countUnreadByGroup(userId, NotificationQueryType.FOLLOW);

        return new UnreadCountVO(replyCount, likeCount, followCount);
    }

    @Override
    public Long getUnreadCountByType(Long userId, String notificationGroup) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "参数不能为空");
        }

        NotificationQueryType queryType = NotificationQueryType.getEnumByValue(notificationGroup);
        if (queryType == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_FORMAT_ERROR);
        }
        return countUnreadByGroup(userId, queryType);
    }

    /**
     * 根据通知分组查询未读数
     */
    private long countUnreadByGroup(Long userId, NotificationQueryType queryType) {
        Long count = notificationMapper.countUnreadByGroup(
                userId,
                queryType.getValue(),
                queryType.getNotificationTypes()
        );
        return count != null ? count : 0L;
    }

    
    @Override
    @Transactional
    public void markGroupAsRead(Long userId, String notificationGroup, Long maxNotificationId) {
        if (userId == null || notificationGroup == null || maxNotificationId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "参数不能为空");
        }
        
        notificationReadStateMapper.markAsRead(userId, notificationGroup, maxNotificationId);
    }
}
