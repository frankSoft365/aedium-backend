package com.microsoft.aediumbackend.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.microsoft.aediumbackend.handler.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("notification")
public class Notification {
    @TableId
    private Long id;
    
    /**
     * 接收者用户ID
     */
    private Long recipientId;
    
    /**
     * 发起者用户ID
     */
    private Long actorId;
    
    /**
     * 通知类型：NEW_COMMENT, NEW_REPLY, LIKE_ARTICLE, LIKE_COMMENT, NEW_FOLLOWER
     */
    private String type;
    
    /**
     * 目标类型：ARTICLE, COMMENT, USER
     */
    private String targetType;
    
    /**
     * 目标ID(关联的主实体ID)
     */
    private Long targetId;
    
    /**
     * 额外参数(存储相关ID等JSON数据)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> params;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
