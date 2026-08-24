package com.microsoft.aediumbackend.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("user_follow")
public class UserFollow {

    private Long followerId;

    private Long followedId;

    private LocalDateTime createTime;
}
