package com.microsoft.aediumbackend.controller;

import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.commen.Result;
import com.microsoft.aediumbackend.exception.BusinessException;
import com.microsoft.aediumbackend.model.dto.comment.request.CreateCommentRequest;
import com.microsoft.aediumbackend.model.dto.comment.request.CursorPageRequest;
import com.microsoft.aediumbackend.model.dto.comment.response.AddCommentResponse;
import com.microsoft.aediumbackend.model.dto.comment.response.CommentThreadDTO;
import com.microsoft.aediumbackend.model.dto.comment.response.CommentView;
import com.microsoft.aediumbackend.model.dto.comment.response.CursorPage;
import com.microsoft.aediumbackend.service.CommentService;
import com.microsoft.aediumbackend.utils.CurrentHold;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.*;

@RestController
@RequestMapping("/article/{articleId}/comment")
public class CommentController {

    @Resource
    private CommentService commentService;

    @PostMapping("/getList")
    public Result<CursorPage<CommentThreadDTO>> getRootComments(
            @PathVariable Long articleId,
            @Valid @RequestBody CursorPageRequest req) {
        if (articleId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_FORMAT_ERROR);
        }

        CursorPage<CommentThreadDTO> page = commentService.getRootComments(articleId, req);
        return Result.success(page);
    }

    @PostMapping("/addComment")
    public Result<AddCommentResponse> addComment(
            @PathVariable Long articleId,
            @RequestBody @Valid CreateCommentRequest req) {
        if (articleId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_FORMAT_ERROR);
        }
        Long userId = CurrentHold.getCurrentId();
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_FORMAT_ERROR);
        }
        AddCommentResponse addCommentResponse = commentService.addComment(articleId, userId, req);
        return Result.success(addCommentResponse);
    }

    @PostMapping("/getRepliesForRoot/{rootId}")
    public Result<CursorPage<CommentView>> getRepliesForRootComment(
            @PathVariable Long articleId,
            @PathVariable Long rootId,
            @RequestBody @Valid CursorPageRequest req) {
        if (articleId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_FORMAT_ERROR);
        }
        if (rootId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_FORMAT_ERROR);
        }
        CursorPage<CommentView> page = commentService.getRepliesForRoot(articleId, rootId, req);
        return Result.success(page);
    }
}
