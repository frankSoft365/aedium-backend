package com.microsoft.aediumbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.exception.BusinessException;
import com.microsoft.aediumbackend.mapper.CommentMapper;
import com.microsoft.aediumbackend.model.dto.comment.request.CreateCommentRequest;
import com.microsoft.aediumbackend.model.dto.comment.request.CursorPageRequest;
import com.microsoft.aediumbackend.model.dto.comment.response.AddCommentResponse;
import com.microsoft.aediumbackend.model.dto.comment.response.CommentThreadDTO;
import com.microsoft.aediumbackend.model.dto.comment.response.CommentView;
import com.microsoft.aediumbackend.model.dto.comment.response.CursorPage;
import com.microsoft.aediumbackend.model.dto.user.response.UserBriefDTO;
import com.microsoft.aediumbackend.model.entity.Article;
import com.microsoft.aediumbackend.model.entity.Comment;
import com.microsoft.aediumbackend.model.enums.CommentStatus;
import com.microsoft.aediumbackend.service.ArticleService;
import com.microsoft.aediumbackend.service.CommentService;
import com.microsoft.aediumbackend.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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
        // 如果size + 1存在 告知size + 1的id 评论时间
        boolean hasMore = rootCommentList.size() > req.getSize();
        if (hasMore) {
            rootCommentList = rootCommentList.subList(0, req.getSize());
        }
        // 查询每个root的前三条评论 findReplyPreviewsForRoots
        List<Long> rootIds = rootCommentList.stream().map(Comment::getId).toList();
        List<Comment> replyPreviewsList = commentMapper.findReplyPreviewsForRoots(rootIds, REPLY_PREVIEW_SIZE);
        Map<Long, List<Comment>> replyPreviewsForRootIdMap = replyPreviewsList.stream()
                .collect(Collectors.groupingBy(Comment::getRootId));
        // 查所有的用户信息
        Set<Long> userIds = Stream.concat(rootCommentList.stream(), replyPreviewsList.stream())
                .map(Comment::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserBriefDTO> usersInfoMap = userService.getUsersBriefByIds(userIds);
        // 将root与root的前三条评论组合一起
        List<CommentThreadDTO> commentThreadDTOList = rootCommentList.stream()
                .map(rootComment ->
                        toCommentThreadDTO(
                                rootComment,
                                replyPreviewsForRootIdMap.getOrDefault(rootComment.getId(), Collections.emptyList()),
                                usersInfoMap
                        )
                ).toList();
        Comment lastComment = rootCommentList.get(rootCommentList.size() - 1);
        // 返回
        return new CursorPage<>(
                commentThreadDTOList,
                hasMore,
                lastComment.getCreateTime(),
                lastComment.getId()
        );
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
        }
        this.save(comment);
        // 如果是回复评论,增加根评论的 reply_count
        if (comment.getRootId() != null) {
            commentMapper.incrementReplyCount(comment.getRootId());
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
        List<Comment> rootAndItsReplies = commentMapper.findRepliesForRoot(
                articleId,
                rootId,
                req.getLastCreatedAt(),
                req.getLastId(),
                req.getSize() + 1
        );
        if (rootAndItsReplies.isEmpty()) {
            return new CursorPage<>(Collections.emptyList(), false, null, null);
        }
        boolean hasMore = rootAndItsReplies.size() > req.getSize();
        if (hasMore) {
            rootAndItsReplies = rootAndItsReplies.subList(0, req.getSize());
        }

        Set<Long> userIds = rootAndItsReplies.stream().map(Comment::getUserId).collect(Collectors.toSet());
        // 获取根评论id
        Comment rootComment = this.getById(rootId);
        if (rootComment == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, COMMENT_NOT_FOUND);
        }
        userIds.add(rootComment.getId());
        Map<Long, UserBriefDTO> usersInfoMap = userService.getUsersBriefByIds(userIds);
        List<CommentView> replyViewList = rootAndItsReplies.stream()
                // 不包含根评论
                .filter(comment -> !Objects.equals(comment.getId(), rootId))
                .map(comment -> toCommentView(comment, usersInfoMap))
                .toList();
        CommentView lastReplyView = replyViewList.get(replyViewList.size() - 1);

        return new CursorPage<CommentView>(
                replyViewList,
                hasMore,
                lastReplyView.getCreatedAt(),
                lastReplyView.getId()
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
}
