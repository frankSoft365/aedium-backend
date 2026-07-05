package com.microsoft.aediumbackend.model.dto.emailVerify;

import lombok.Data;

@Data
public class VerifyCodeRequest {
    private String email;
    private String verifyCode;
}
