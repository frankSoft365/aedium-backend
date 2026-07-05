package com.microsoft.aediumbackend.service;

import com.microsoft.aediumbackend.model.dto.emailVerify.SendCodeRequest;
import com.microsoft.aediumbackend.model.dto.emailVerify.VerifyCodeRequest;

public interface EmailVerifyService {
    void sendVerificationCode(SendCodeRequest request);

    String verifyVerificationCode(VerifyCodeRequest request);
}
