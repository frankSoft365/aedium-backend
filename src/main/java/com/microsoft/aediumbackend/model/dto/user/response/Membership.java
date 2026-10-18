package com.microsoft.aediumbackend.model.dto.user.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Membership {
    private LocalDateTime memberSince;
}
