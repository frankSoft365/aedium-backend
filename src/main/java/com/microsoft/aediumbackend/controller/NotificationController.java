package com.microsoft.aediumbackend.controller;

import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.commen.Result;
import com.microsoft.aediumbackend.exception.BusinessException;
import com.microsoft.aediumbackend.commen.CursorPageRequest;
import com.microsoft.aediumbackend.model.dto.notification.response.NotificationCursorPage;
import com.microsoft.aediumbackend.model.dto.notification.response.NotificationVO;
import com.microsoft.aediumbackend.model.dto.notification.response.UnreadCountVO;
import com.microsoft.aediumbackend.model.enums.NotificationQueryType;
import com.microsoft.aediumbackend.service.NotificationService;
import com.microsoft.aediumbackend.utils.CurrentHold;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.NOTIFICATION_TYPE_INVALID;
import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.PARAM_FORMAT_ERROR;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/unread-count")
    public Result<UnreadCountVO> getUnreadCount() {
        Long currentId = CurrentHold.getCurrentId();
        if (currentId == null || currentId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_FORMAT_ERROR);
        }
        UnreadCountVO unreadCount = notificationService.getUnreadCount(currentId);
        return Result.success(unreadCount);
    }

    @PostMapping("/getList")
    public Result<NotificationCursorPage<NotificationVO>> getNotificationByType(
            @RequestParam String type,
            @RequestParam(required = false) Long watermark,
            @Valid @RequestBody CursorPageRequest req) {
        Long currentId = CurrentHold.getCurrentId();
        if (currentId == null || currentId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_FORMAT_ERROR);
        }

        NotificationQueryType queryType = NotificationQueryType.getEnumByValue(type);
        if (queryType == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, NOTIFICATION_TYPE_INVALID);
        }

        NotificationCursorPage<NotificationVO> notificationsByType = notificationService.getNotificationsByType(currentId, req, queryType, watermark);
        return Result.success(notificationsByType);

    }
    
    @PostMapping("/mark-read")
    public Result<Void> markGroupAsRead(
            @RequestParam String type,
            @RequestParam Long maxNotificationId) {
        Long currentId = CurrentHold.getCurrentId();
        if (currentId == null || currentId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_FORMAT_ERROR);
        }
        
        NotificationQueryType queryType = NotificationQueryType.getEnumByValue(type);
        if (queryType == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, NOTIFICATION_TYPE_INVALID);
        }
        
        notificationService.markGroupAsRead(currentId, queryType.getValue(), maxNotificationId);
        return Result.success();
    }
}
