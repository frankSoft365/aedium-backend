package com.microsoft.aediumbackend.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户
 */
@Data
@TableName("`user`")
public class User {
    private Long id;// 主键id
    private String username;// 用户名
    private String password;// 密码
    private String image;// 头像的url
    private LocalDateTime createTime;// 创建时间
    private LocalDateTime updateTime;// 修改时间
    private Integer isDelete;// 是否删除 0 未被删除 1 被删除 默认不被删除
    private String email;// 电子邮件
    private Integer userRole;// 用户权限
}
