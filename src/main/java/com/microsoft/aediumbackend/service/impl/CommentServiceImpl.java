package com.microsoft.aediumbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.exception.BusinessException;
import com.microsoft.aediumbackend.mapper.CommentMapper;
import com.microsoft.aediumbackend.model.dto.comment.request.CreateCommentRequest;
import com.microsoft.aediumbackend.commen.CursorPageRequest;
import com.microsoft.aediumbackend.model.dto.comment.response.AddCommentResponse;
import com.microsoft.aediumbackend.model.dto.comment.response.CommentBriefDTO;
import com.microsoft.aediumbackend.model.dto.comment.response.CommentThreadDTO;
import com.microsoft.aediumbackend.model.dto.comment.response.CommentView;
import com.microsoft.aediumbackend.commen.CursorPage;
import com.microsoft.aediumbackend.model.dto.notification.response.ReplyNotificationVO;
import com.microsoft.aediumbackend.model.dto.notification.response.UnreadCountVO;
import com.microsoft.aediumbackend.model.dto.notificationPush.request.NotificationPushRequest;
import com.microsoft.aediumbackend.model.dto.user.response.UserBriefDTO;
import com.microsoft.aediumbackend.model.entity.Article;
import com.microsoft.aediumbackend.model.entity.Comment;
import com.microsoft.aediumbackend.model.entity.Notification;
import com.microsoft.aediumbackend.model.enums.CommentStatus;
import com.microsoft.aediumbackend.model.enums.NotificationQueryType;
import com.microsoft.aediumbackend.model.enums.NotificationTargetType;
import com.microsoft.aediumbackend.model.enums.NotificationType;
import com.microsoft.aediumbackend.service.ArticleService;
import com.microsoft.aediumbackend.service.CommentService;
import com.microsoft.aediumbackend.service.NotificationService;
import com.microsoft.aediumbackend.service.UserService;
import com.microsoft.aediumbackend.service.impl.notification.NotificationPushService;
import com.microsoft.aediumbackend.utils.CursorPageUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.*;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {
    @Resource
    private CommentMapper commentMapper;
    @Resource
    private UserService userService;

    private static final int REPLY_PREVIEW_SIZE = 3;

    @Resource
    private ArticleService articleService;

    @Lazy
    @Resource
    private NotificationService notificationService;

    @Resource
    private NotificationPushService notificationPushService;

    /**
     * 获取评论列表
     *
     * @param articleId 对应文章id
     * @return 评论列表分页结果
     */
    @Override
    public CursorPage<CommentThreadDTO> getRootComments(Long articleId, CursorPageRequest req) {
        Article article = articleService.getById(articleId);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, ARTICLE_NOT_FOUND);
        }
        // 查询目标size的根评论 查询size + 1
        List<Comment> rootCommentList = commentMapper.findRootCommentsCursor(
                articleId,
                req.getLastCreatedAt(),
                req.getLastId(),
                req.getSize() + 1
        );
        if (rootCommentList.isEmpty()) {
            return new CursorPage<>(Collections.emptyList(), false, null, null);
        }
        
        CursorPageUtils.CursorInfo cursorInfo = CursorPageUtils.extract(
                rootCommentList, req.getSize(), Comment::getCreateTime, Comment::getId);

        List<CommentThreadDTO> commentThreadDTOList = getRootCommentsCore(rootCommentList, false, null);

        return new CursorPage<>(
                commentThreadDTOList,
                cursorInfo.isHasMore(),
                cursorInfo.getNextCursorCreatedAt(),
                cursorInfo.getNextCursorId()
        );
    }

    private List<CommentThreadDTO> getRootCommentsCore(List<Comment> rootCommentList, boolean isContext, List<Long> contextIds) {
        // 查询每个root的前三条评论 findReplyPreviewsForRoots
        List<Long> rootIds = rootCommentList.stream().map(Comment::getId).toList();
        List<Comment> replyPreviewsList = isContext && contextIds != null ?
                commentMapper.findReplyPreviewsContextForRootByReplyId(contextIds)
                : commentMapper.findReplyPreviewsForRoots(rootIds, REPLY_PREVIEW_SIZE);
        Map<Long, List<Comment>> replyPreviewsForRootIdMap = replyPreviewsList.stream()
                .collect(Collectors.groupingBy(Comment::getRootId));
        // 查所有的用户信息
        Set<Long> userIds = Stream.concat(rootCommentList.stream(), replyPreviewsList.stream())
                .map(Comment::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserBriefDTO> usersInfoMap = userService.getUsersBriefByIds(userIds);
        // 将root与root的前三条评论组合一起
        return rootCommentList.stream()
                .map(rootComment ->
                        toCommentThreadDTO(
                                rootComment,
                                replyPreviewsForRootIdMap.getOrDefault(rootComment.getId(), Collections.emptyList()),
                                usersInfoMap
                        )
                ).toList();
    }

    @Override
    public CommentThreadDTO getRootCommentAndContextById(Long replyId) {
        Comment comment = commentMapper.selectById(replyId);
        List<Comment> rootCommentList = new ArrayList<>();
        CommentThreadDTO res = null;
        if (comment.getRootId() == null) {
            // 根评论
            rootCommentList.add(comment);
            List<CommentThreadDTO> commentThreadDTO = getRootCommentsCore(rootCommentList, false, null);
            res = commentThreadDTO.get(0);
        } else {
            Long rootId = comment.getRootId();
            Long parentId = comment.getParentId();
            Comment rootComment = commentMapper.selectById(rootId);
            rootCommentList.add(rootComment);
            List<Long> ids = new ArrayList<>();
            if (!parentId.equals(rootId)) {
                ids.add(parentId);
            }
            ids.add(replyId);
            List<CommentThreadDTO> commentThreadDTO = getRootCommentsCore(rootCommentList, true, ids);
            res = commentThreadDTO.get(0);
        }
        res.setPinned(true);
        return res;
    }

    /**
     * TODO 限流评论频率 内容审核 幂等防重评论
     */
    @Override
    @Transactional
    public AddCommentResponse addComment(Long articleId, Long userId, CreateCommentRequest req) {
        Article byId = articleService.getById(articleId);
        if (byId == null) {
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
            Comment parentComment = this.getById(parentId);
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

        this.save(comment);

        boolean isReply = comment.getRootId() != null;

        // 如果是回复评论,增加根评论的 reply_count
        if (isReply) {
            commentMapper.incrementReplyCount(comment.getRootId());
        }

        // 持久化一条通知 notification
        Article article = articleService.getById(articleId);
        Map<String, Object> params = new HashMap<>();
        Notification notification = new Notification();
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
        // 向在线用户推送这条通知
        if (notification != null) {
            ReplyNotificationVO replyNotificationVO = notificationService.toReplyNotificationVO(notification);
            Long recipientId = replyNotificationVO.getRecipientId();
            Long unreadReplyCount = notificationService.getUnreadCountByType(recipientId, NotificationQueryType.REPLY.getValue());
            NotificationPushRequest<ReplyNotificationVO> request = new NotificationPushRequest<>(
                    recipientId,
                    NotificationQueryType.REPLY.getValue(),
                    unreadReplyCount,
                    replyNotificationVO
            );
            notificationPushService.pushUnreadCount(request);
        }

        // 构建返回数据
        HashSet<Long> userIdSet = new HashSet<>();
        userIdSet.add(userId);
        if (comment.getReplyToUserId() != null) {
            userIdSet.add(comment.getReplyToUserId());
        }
        Map<Long, UserBriefDTO> userInfoMap = userService.getUsersBriefByIds(userIdSet);
        return new AddCommentResponse(toCommentView(comment, userInfoMap), comment.getRootId());
    }

    @Override
    public CursorPage<CommentView> getRepliesForRoot(Long articleId, Long rootId, CursorPageRequest req) {
        Article article = articleService.getById(articleId);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, ARTICLE_NOT_FOUND);
        }
        List<Comment> repliesForRoot = commentMapper.findRepliesForRoot(
                articleId,
                rootId,
                req.getLastCreatedAt(),
                req.getLastId(),
                req.getSize() + 1
        );
        if (repliesForRoot.isEmpty()) {
            return new CursorPage<>(Collections.emptyList(), false, null, null);
        }
        
        CursorPageUtils.CursorInfo cursorInfo = CursorPageUtils.extract(
                repliesForRoot, req.getSize(), Comment::getCreateTime, Comment::getId);

        Set<Long> userIds = repliesForRoot.stream().map(Comment::getUserId).collect(Collectors.toSet());
        // 获取根评论id
        Comment rootComment = this.getById(rootId);
        if (rootComment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, COMMENT_NOT_FOUND);
        }
        userIds.add(rootComment.getId());
        Map<Long, UserBriefDTO> usersInfoMap = userService.getUsersBriefByIds(userIds);
        List<CommentView> replyViewList = repliesForRoot.stream()
                .map(comment -> toCommentView(comment, usersInfoMap))
                .toList();

        return new CursorPage<CommentView>(
                replyViewList,
                cursorInfo.isHasMore(),
                cursorInfo.getNextCursorCreatedAt(),
                cursorInfo.getNextCursorId()
        );
    }

    private CommentThreadDTO toCommentThreadDTO(Comment rootComment, List<Comment> repliesForRoot, Map<Long, UserBriefDTO> usersInfoMap) {
        CommentView rootCommentView = toCommentView(rootComment, usersInfoMap);
        List<CommentView> repliesViewForRoot = repliesForRoot.stream()
                .map(reply -> toCommentView(reply, usersInfoMap))
                .toList();
        return new CommentThreadDTO(
                rootCommentView,
                repliesViewForRoot,
                rootComment.getReplyCount(),
                rootComment.getReplyCount() > REPLY_PREVIEW_SIZE
        );
    }

    private CommentView toCommentView(Comment comment, Map<Long, UserBriefDTO> usersInfoMap) {
        UserBriefDTO commentAuthor = usersInfoMap.getOrDefault(comment.getUserId(), new UserBriefDTO());
        
        UserBriefDTO replyToUserInfo = comment.getReplyToUserId() != null ? usersInfoMap.get(comment.getReplyToUserId()) : null;

        boolean isDeleted = CommentStatus.DELETED.equals(CommentStatus.getEnumByValue(comment.getStatus()));

        return new CommentView(
                comment.getId(),
                comment.getUserId(),
                isDeleted ? null : commentAuthor.getUsername(),
                isDeleted ? null : commentAuthor.getImage(),
                isDeleted ? "" : comment.getContent(),
                comment.getParentId(),
                comment.getReplyToUserId(),
                replyToUserInfo != null ? replyToUserInfo.getUsername() : null,
                comment.getCreateTime()
        );
    }

    @Override
    public Map<Long, CommentBriefDTO> getCommentBriefByIds(Set<Long> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) {
            return Map.of();
        }
        List<CommentBriefDTO> commentBriefList = commentMapper.getCommentBriefByIds(commentIds);
        return commentBriefList.stream()
                .collect(Collectors.toMap(CommentBriefDTO::getId, Function.identity()));
    }
}
