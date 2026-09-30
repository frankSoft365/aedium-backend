package com.microsoft.aediumbackend.controller;

import com.microsoft.aediumbackend.commen.CursorPage;
import com.microsoft.aediumbackend.commen.DeleteRequest;
import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.commen.Result;
import com.microsoft.aediumbackend.exception.BusinessException;
import com.microsoft.aediumbackend.model.dto.readingHistory.request.ReadingHistoryQuery;
import com.microsoft.aediumbackend.model.dto.readingHistory.request.RecordReadingHistoryRequest;
import com.microsoft.aediumbackend.model.vo.ArticleListItemVO;
import com.microsoft.aediumbackend.service.readingHistory.ReadingHistoryService;
import com.microsoft.aediumbackend.utils.CurrentHold;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.NO_PERMISSION;

@RestController
@RequestMapping("/reading-history")
public class ReadingHistoryController {
    private final ReadingHistoryService readingHistoryService;

    public ReadingHistoryController(ReadingHistoryService readingHistoryService) {
        this.readingHistoryService = readingHistoryService;
    }

    /**
     * 记录用户的一条阅读历史
     */
    @PostMapping("/save")
    public Result<Void> saveReadingHistory(
            @Valid @RequestBody
            RecordReadingHistoryRequest req
    ) {
        Long userId = CurrentHold.getCurrentId();
        if (userId == null || userId < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, NO_PERMISSION);
        }
        readingHistoryService.saveReadingHistory(
                userId,
                req.getArticleId()
        );
        return Result.success();
    }

    /**
     * 查询用户的阅读历史：文章预览列表（游标控制，最后阅读时间降序排列）
     */
    @PostMapping("/list")
    public Result<CursorPage<ArticleListItemVO>> getReadingHistoryList(
            @Valid @RequestBody
            ReadingHistoryQuery query
    ) {
        Long userId = CurrentHold.getCurrentId();
        if (userId == null || userId < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, NO_PERMISSION);
        }
        return Result.success(readingHistoryService.getReadingHistoryList(
                userId,
                query
        ));
    }

    /**
     * 清空用户的所有阅读历史
     */
    @PostMapping("/clear")
    public Result<Void> clearAllReadingHistory() {
        Long userId = CurrentHold.getCurrentId();
        if (userId == null || userId < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, NO_PERMISSION);
        }
        readingHistoryService.clearAllReadingHistory(userId);
        return Result.success();
    }

    /**
     * 删除用户指定的一条阅读历史
     */
    @PostMapping("/delete")
    public Result<Void> deleteOneReadingHistory(
            @Valid @RequestBody
            DeleteRequest req
    ) {
        Long userId = CurrentHold.getCurrentId();
        if (userId == null || userId < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, NO_PERMISSION);
        }
        readingHistoryService.deleteOneReadingHistory(userId, req.getId());
        return Result.success();
    }
}
