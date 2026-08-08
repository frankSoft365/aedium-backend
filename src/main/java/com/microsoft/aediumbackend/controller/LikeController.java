package com.microsoft.aediumbackend.controller;

import com.microsoft.aediumbackend.commen.Result;
import com.microsoft.aediumbackend.model.dto.like.LikeActionRequest;
import com.microsoft.aediumbackend.model.dto.like.LikeBatchStatusRequest;
import com.microsoft.aediumbackend.model.dto.like.LikeBatchStatusResult;
import com.microsoft.aediumbackend.service.UserLikeService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/like")
public class LikeController {

    @Resource
    private UserLikeService userLikeService;

    @PostMapping("/action")
    public Result<Void> handleAction(@RequestBody LikeActionRequest request) {
        userLikeService.handleAction(request);
        return Result.success();
    }

    @PostMapping("/batch-status")
    public Result<LikeBatchStatusResult> batchStatus(@RequestBody LikeBatchStatusRequest request) {
        LikeBatchStatusResult result = userLikeService.batchStatus(request);
        return Result.success(result);
    }
}