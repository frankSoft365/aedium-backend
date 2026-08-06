package com.microsoft.aediumbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
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
import com.microsoft.aediumbackend.model.dto.user.response.UserBriefDTO;
import com.microsoft.aediumbackend.model.entity.Article;
import com.microsoft.aediumbackend.model.entity.Comment;
import com.microsoft.aediumbackend.model.entity.Notification;
import com.microsoft.aediumbackend.model.enums.CommentStatus;
import com.microsoft.aediumbackend.service.*;
import com.microsoft.aediumbackend.service.impl.comment.CommentPersistService;
import com.microsoft.aediumbackend.service.impl.comment.CommentPostProcessService;
import com.microsoft.aediumbackend.utils.CursorPageUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Duration;
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
    @Resource
    private ArticleService articleService;
    @Lazy
    @Resource
    private CommentPostProcessService commentPostProcessService;
    @Resource
    private RedisCacheService redisCacheService;
    @Lazy
    @Resource
    private CommentPersistService commentPersistService;

    private static final int REPLY_PREVIEW_SIZE = 3;
    public static final Duration COMMENT_FIRST_PAGE_TTL = Duration.ofSeconds(30);
    public static final String COMMENTS_ROOT_FIRST = "comments:root:first:";

    @Value("${cache.comments.enabled:true}")
    private boolean cacheEnabled;

    /**
     * 获取评论列表
     * @param articleId 对应文章id
     * @return 评论列表分页结果
     */
    @Override
    public CursorPage<CommentThreadDTO> getRootComments(Long articleId, CursorPageRequest req) {
        Article article = articleService.getById(articleId);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, ARTICLE_NOT_FOUND);
        }
        // 首页评论
        if (cacheEnabled && req.getLastCreatedAt() == null) {
            String key = COMMENTS_ROOT_FIRST + articleId;
            return redisCacheService.get(key, new TypeReference<CursorPage<CommentThreadDTO>>() {
                    })
                    .orElseGet(() -> {
                        CursorPage<CommentThreadDTO> firstPage = getRootCommentsCore(articleId, req);
                        redisCacheService.set(key, firstPage, COMMENT_FIRST_PAGE_TTL);
                        return firstPage;
                    });
        }
        // 查询目标size的根评论 查询size + 1
        return getRootCommentsCore(articleId, req);
    }

    public CursorPage<CommentThreadDTO> getRootCommentsCore(Long articleId, CursorPageRequest req) {
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

        List<CommentThreadDTO> commentThreadDTOList = getCommentThreadDTO(rootCommentList, false, null);

        return new CursorPage<>(
                commentThreadDTOList,
                cursorInfo.isHasMore(),
                cursorInfo.getNextCursorCreatedAt(),
                cursorInfo.getNextCursorId()
        );
    }

    private List<CommentThreadDTO> getCommentThreadDTO(List<Comment> rootCommentList, boolean isContext, List<Long> contextIds) {
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
        Map<Long, UserBriefDTO> usersInfoMap = !userIds.isEmpty() ? userService.getUsersBriefByIds(userIds) : Collections.emptyMap();
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

    /**
     * 获取评论类型通知的根评论及回复上下文
     * @param replyId 目标评论的id 根评论或者回复
     * @return 返回整个包含根评论及回复的上下文
     */
    @Override
    public CommentThreadDTO getRootCommentAndContextById(Long replyId) {
        Comment targetComment = commentMapper.selectById(replyId);
        List<Comment> rootCommentList = new ArrayList<>();
        CommentThreadDTO res = null;
        if (targetComment.getRootId() == null) {
            // 根评论
            rootCommentList.add(targetComment);
            List<CommentThreadDTO> commentThreadDTO = getCommentThreadDTO(rootCommentList, false, null);
            res = commentThreadDTO.get(0);
        } else {
            Long rootId = targetComment.getRootId();
            Long parentId = targetComment.getParentId();
            Comment rootComment = commentMapper.selectById(rootId);
            rootCommentList.add(rootComment);
            List<Long> ids = new ArrayList<>();
            if (!parentId.equals(rootId)) {
                ids.add(parentId);
            }
            ids.add(replyId);
            List<CommentThreadDTO> commentThreadDTO = getCommentThreadDTO(rootCommentList, true, ids);
            res = commentThreadDTO.get(0);
        }
        res.setPinned(true);
        return res;
    }

    /**
     * TODO 限流评论频率 内容审核 幂等防重评论
     * TODO reply_count hot-row contention becomes real at this scale
     * TODO Idempotency — necessary once you introduce a queue/retry mechanism
     */
    @Override
    public AddCommentResponse addComment(Long articleId, Long userId, CreateCommentRequest req) {
        // 作为事务的持久化评论和通知
        CommentPersistService.AddCommentRes addCommentRes = commentPersistService.persistComment(articleId, userId, req);
        Comment comment = addCommentRes.getComment();
        Notification notification = addCommentRes.getNotification();

        // 异步 刷新缓存 消息推送
        commentPostProcessService.handlePostCommentTasks(comment, articleId, notification);

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
