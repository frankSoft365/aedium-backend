package com.microsoft.aediumbackend.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("`user_reading_history`")
public class UserReadingHistory {
    private Long userId;
    private Long articleId;
    private LocalDateTime lastReadingTime;
}
