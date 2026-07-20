package com.microsoft.aediumbackend.exception;


import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.commen.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.FILE_SIZE_EXCEEDED;
import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.PARAM_INVALID;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public Result<Void> runtimeExceptionHandler(RuntimeException e) {
        log.error("系统内部错误", e);
        return Result.error(ErrorCode.SYSTEM_ERROR, "系统内部错误");
    }

    @ExceptionHandler(BusinessException.class)
    public Result<Void> businessExceptionHandler(BusinessException e) {
//        log.error("业务错误", e);
        log.error("业务错误详情：errorCode : {} | message : {} | description : {}", e.getCode(), e.getMessage(), e.getDescription());
        return Result.error(e.getCode(), e.getMessage(), e.getDescription());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> MaxUploadSizeExceededExceptionHandler(MaxUploadSizeExceededException e) {
        log.error("上传文件大小超出限度", e);
        return Result.error(ErrorCode.PARAM_ERROR, FILE_SIZE_EXCEEDED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse(PARAM_INVALID);
        log.warn("参数校验失败: {}", message);
        return Result.error(ErrorCode.PARAM_ERROR, message);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse(PARAM_INVALID);
        log.warn("参数绑定失败: {}", message);
        return Result.error(ErrorCode.PARAM_ERROR, message);
    }
}
