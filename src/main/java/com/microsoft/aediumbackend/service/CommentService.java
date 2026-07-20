package com.microsoft.aediumbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.microsoft.aediumbackend.model.dto.comment.request.CreateCommentRequest;
import com.microsoft.aediumbackend.model.dto.comment.request.CursorPageRequest;
import com.microsoft.aediumbackend.model.dto.comment.response.AddCommentResponse;
import com.microsoft.aediumbackend.model.dto.comment.response.CommentThreadDTO;
import com.microsoft.aediumbackend.model.dto.comment.response.CommentView;
import com.microsoft.aediumbackend.model.dto.comment.response.CursorPage;
import com.microsoft.aediumbackend.model.entity.Comment;
import jakarta.validation.Valid;

public interface CommentService extends IService<Comment> {

    CursorPage<CommentThreadDTO> getRootComments(Long articleId, CursorPageRequest req);

    AddCommentResponse addComment(Long articleId, Long userId, CreateCommentRequest req);

    CursorPage<CommentView> getRepliesForRoot(Long articleId, Long rootId, CursorPageRequest req);
}
