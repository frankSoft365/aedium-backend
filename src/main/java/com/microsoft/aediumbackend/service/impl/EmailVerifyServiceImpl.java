package com.microsoft.aediumbackend.service.impl;

import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.exception.BusinessException;
import com.microsoft.aediumbackend.model.dto.emailVerify.SendCodeRequest;
import com.microsoft.aediumbackend.model.dto.emailVerify.VerifyCodeRequest;
import com.microsoft.aediumbackend.service.EmailVerifyService;
import com.microsoft.aediumbackend.utils.RegexUtils;
import com.microsoft.frankapisdk.client.FrankApiClient;
import com.microsoft.frankapisdk.commen.ApiResponse;
import com.microsoft.frankapisdk.commen.ErrorDescription;
import com.microsoft.frankapisdk.commen.ErrorResponse;
import com.microsoft.frankapisdk.model.SendMailRequest;
import com.microsoft.frankapisdk.model.response.SendMailResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.*;

@Slf4j
@Service
public class EmailVerifyServiceImpl implements EmailVerifyService {

    @Resource
    private FrankApiClient frankApiClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void sendVerificationCode(SendCodeRequest request) {
        String email = request.getEmail();
        if (!RegexUtils.isValidEmail(email)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, EMAIL_FORMAT_INVALID);
        }
        String key = getEmailCodeKey(email);
        String existCode = stringRedisTemplate.opsForValue().get(key);
        if (existCode != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, VERIFY_CODE_SENT_NO_REPEAT);
        }
        SendMailRequest sendMailRequest = new SendMailRequest(email, "Aedium_official", "Aedium register verify code");
        ApiResponse apiResponse = frankApiClient.callSendMailResponse(sendMailRequest);

        if (!(apiResponse instanceof SendMailResponse)) {
            ErrorResponse errorResponse = (ErrorResponse) apiResponse;
            ErrorDescription errorDescription = errorResponse.getError();
            String errorCode = errorDescription.getCode();
            String errorMessage = errorDescription.getMessage();
            if (com.microsoft.frankapisdk.commen.ErrorCode.LimitExceeded.equals(com.microsoft.frankapisdk.commen.ErrorCode.getByCode(errorCode))) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, EMAIL_VERIFY_CODE_SEND_FAILED);
        }
        String verifyCode = ((SendMailResponse) apiResponse).getVerifyCode();
        log.info("发送验证码 : {}", verifyCode);
        stringRedisTemplate.opsForValue().set(key, verifyCode, 5,TimeUnit.MINUTES);
    }

    @Override
    public String verifyVerificationCode(VerifyCodeRequest request) {
        String email = request.getEmail();
        String verifyCode = request.getVerifyCode();
        // 拿出redis的值
        String emailCodeKey = getEmailCodeKey(email);
        String originVerifyCode = stringRedisTemplate.opsForValue().get(emailCodeKey);
        // 若没有
        if (originVerifyCode == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, VERIFY_CODE_INVALID);
        }
        // 若有
        // 不一致
        if (!originVerifyCode.equals(verifyCode)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, VERIFY_CODE_INCORRECT);
        }
        // 一致
        // 删redis
        stringRedisTemplate.delete(emailCodeKey);
        // 生成token
        String token = UUID.randomUUID().toString();
        // 存token
        String tokenEmailKey = getTokenEmailKey(token);
        stringRedisTemplate.opsForValue().set(tokenEmailKey, email, 3, TimeUnit.MINUTES);
        return token;
    }

    private String getEmailCodeKey(String email) {
        return "register:code:" + email;
    }

    public static String getTokenEmailKey(String token) {
        return "register:token:"+ token;
    }
}
