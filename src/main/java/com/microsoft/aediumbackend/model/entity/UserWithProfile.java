package com.microsoft.aediumbackend.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserWithProfile {
    private Long id;// 主键id
    private String username;// 用户名
    private String image;// 头像的url
    private LocalDateTime createTime;// 创建时间
    private Integer followerCount;// 粉丝数量
    private Integer followingCount;// 关注用户数量

    private String bio;
    private String about;
}
