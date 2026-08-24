package com.microsoft.aediumbackend.controller;

import com.microsoft.aediumbackend.commen.CursorPage;
import com.microsoft.aediumbackend.commen.CursorPageRequest;
import com.microsoft.aediumbackend.commen.Result;
import com.microsoft.aediumbackend.model.dto.follow.FollowActionRequest;
import com.microsoft.aediumbackend.model.dto.follow.FollowBatchStatusRequest;
import com.microsoft.aediumbackend.model.dto.follow.FollowBatchStatusResult;
import com.microsoft.aediumbackend.model.vo.UserVO;
import com.microsoft.aediumbackend.service.UserFollowService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/follow")
public class FollowController {

    private final UserFollowService userFollowService;

    public FollowController(UserFollowService userFollowService) {
        this.userFollowService = userFollowService;
    }

    @PostMapping("/action")
    public Result<Void> handleAction(@Valid @RequestBody FollowActionRequest request) {
        userFollowService.handleAction(request);
        return Result.success();
    }

    @PostMapping("/batch-status")
    public Result<FollowBatchStatusResult> batchStatus(@Valid @RequestBody FollowBatchStatusRequest request) {
        return Result.success(userFollowService.batchStatus(request));
    }

    @GetMapping("/followers")
    public Result<CursorPage<UserVO>> getFollowers(
            @RequestParam Long userId,
            @Valid CursorPageRequest request
    ) {
        return Result.success(userFollowService.getFollowers(userId, request));
    }

    @GetMapping("/following")
    public Result<CursorPage<UserVO>> getFollowing(
            @RequestParam Long userId,
            @Valid CursorPageRequest request
    ) {
        return Result.success(userFollowService.getFollowing(userId, request));
    }
}
