package com.microsoft.aediumbackend.controller;

import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.commen.Result;
import com.microsoft.aediumbackend.exception.BusinessException;
import com.microsoft.aediumbackend.model.dto.emailVerify.SendCodeRequest;
import com.microsoft.aediumbackend.model.dto.emailVerify.VerifyCodeRequest;
import com.microsoft.aediumbackend.service.EmailVerifyService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.*;

@RestController
@RequestMapping("/emailVerify")
@Slf4j
public class EmailVerifyController {

    @Resource
    private EmailVerifyService emailVerifyService;

    /**
     * 发送验证码
     */
    @PostMapping("/getVerificationCode")
    public Result<Void> getVerificationCode(@RequestBody SendCodeRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_EMPTY);
        }
        emailVerifyService.sendVerificationCode(request);
        return Result.success();
    }

    /**
     * 校验验证码
     */
    @PostMapping("/verifyVerificationCode")
    public Result<String> verifyVerificationCode(@RequestBody VerifyCodeRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_EMPTY);
        }
        String token = emailVerifyService.verifyVerificationCode(request);
        // 发token
        return Result.success(token);
    }

}
