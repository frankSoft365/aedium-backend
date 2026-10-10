package com.microsoft.aediumbackend.controller;

import com.microsoft.aediumbackend.commen.CursorPage;
import com.microsoft.aediumbackend.commen.DeleteRequest;
import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.commen.Result;
import com.microsoft.aediumbackend.exception.BusinessException;
import com.microsoft.aediumbackend.model.dto.repost.request.*;
import com.microsoft.aediumbackend.model.dto.repost.response.RepostCount;
import com.microsoft.aediumbackend.model.dto.repost.response.RepostListItem;
import com.microsoft.aediumbackend.model.dto.repost.response.RepostState;
import com.microsoft.aediumbackend.service.repost.RepostService;
import com.microsoft.aediumbackend.utils.CurrentHold;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.PARAM_INVALID;

@RestController
@RequestMapping("/repost")
public class RepostController {

    private final RepostService repostService;

    public RepostController(RepostService repostService) {
        this.repostService = repostService;
    }

    @PostMapping("/count")
    public Result<List<RepostCount>> getRepostCountByIds(
            @RequestBody RepostCountQuery query
    ) {
        return Result.success(repostService.getRepostCountByArticleIds(query.getIds()));
    }

    @PostMapping("/state")
    public Result<RepostState> getRepostState(
            @Valid @RequestBody RepostStateQuery query
    ) {
        Long userId = CurrentHold.getCurrentId();
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_INVALID);
        }
        return Result.success(repostService.getRepostStateForUser(
                userId,
                query.getArticleId()
        ));
    }

    @PostMapping("/post")
    public Result<Void> repostAnArticle(
            @Valid @RequestBody RepostRequest req
    ) {
        Long userId = CurrentHold.getCurrentId();
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_INVALID);
        }
        repostService.repostAnArticle(userId, req);
        return Result.success();
    }

    @PostMapping("/delete")
    public Result<Void> deleteRepost(@Valid @RequestBody DeleteRequest req) {
        Long userId = CurrentHold.getCurrentId();
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_INVALID);
        }
        repostService.deleteRepost(userId, req.getId());
        return Result.success();
    }

    @PostMapping("/addNote")
    public Result<Void> addNote(@Valid @RequestBody AddNoteRequest req) {
        Long userId = CurrentHold.getCurrentId();
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_INVALID);
        }
        repostService.addNote(userId, req.getArticleId(), req.getNote());
        return Result.success();
    }

    @PostMapping("/editNote")
    public Result<Void> editNote(@Valid @RequestBody EditNoteRequest req) {
        Long userId = CurrentHold.getCurrentId();
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_INVALID);
        }
        repostService.editNote(userId, req.getArticleId(), req.getNote());
        return Result.success();
    }

    @PostMapping("/removeNote")
    public Result<Void> removeNote(@Valid @RequestBody RemoveNoteRequest req) {
        Long userId = CurrentHold.getCurrentId();
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_INVALID);
        }
        repostService.removeNote(userId, req.getArticleId());
        return Result.success();
    }

    @PostMapping("/list")
    public Result<CursorPage<RepostListItem>> getRepostList(
            @Valid @RequestBody UserRepostListQuery query
    ) {
        return Result.success(repostService.getRepostList(query));
    }
}
