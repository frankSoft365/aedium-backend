package com.microsoft.aediumbackend.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("notification_read_state")
public class NotificationReadState {
    private Long recipientId;
    
    private String notificationGroup;
    
    private Long lastReadId;
    
    private LocalDateTime lastReadAt;
}
