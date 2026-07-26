package com.microsoft.aediumbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.microsoft.aediumbackend.model.dto.comment.request.CreateCommentRequest;
import com.microsoft.aediumbackend.commen.CursorPageRequest;
import com.microsoft.aediumbackend.model.dto.comment.response.AddCommentResponse;
import com.microsoft.aediumbackend.model.dto.comment.response.CommentBriefDTO;
import com.microsoft.aediumbackend.model.dto.comment.response.CommentThreadDTO;
import com.microsoft.aediumbackend.model.dto.comment.response.CommentView;
import com.microsoft.aediumbackend.commen.CursorPage;
import com.microsoft.aediumbackend.model.entity.Comment;

import java.util.Map;
import java.util.Set;

public interface CommentService extends IService<Comment> {

    CursorPage<CommentThreadDTO> getRootComments(Long articleId, CursorPageRequest req);

    /**
     * 查询指定根评论及其上下文
     */
    CommentThreadDTO getRootCommentAndContextById(Long replyId);

    AddCommentResponse addComment(Long articleId, Long userId, CreateCommentRequest req);

    CursorPage<CommentView> getRepliesForRoot(Long articleId, Long rootId, CursorPageRequest req);

    /**
     * 根据ID列表获取评论简要信息
     */
    Map<Long, CommentBriefDTO> getCommentBriefByIds(Set<Long> commentIds);
}
