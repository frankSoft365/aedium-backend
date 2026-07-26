package com.microsoft.aediumbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.microsoft.aediumbackend.model.entity.NotificationReadState;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationReadStateMapper extends BaseMapper<NotificationReadState> {
    
    /**
     * 获取用户指定分组的最后阅读通知ID
     */
    Long getLastReadId(Long recipientId, String notificationGroup);
    
    /**
     * 标记已读(插入或更新最后阅读ID)
     */
    void markAsRead(Long recipientId, String notificationGroup, Long lastReadId);
}
