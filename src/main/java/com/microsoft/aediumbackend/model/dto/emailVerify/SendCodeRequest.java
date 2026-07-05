package com.microsoft.aediumbackend.model.dto.emailVerify;

import lombok.Data;

@Data
public class SendCodeRequest {
    private String email;
}
