package com.microsoft.aediumbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.microsoft.aediumbackend.commen.CursorPageRequest;
import com.microsoft.aediumbackend.model.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 根据通知类型列表查询通知
     */
    List<Notification> findNotificationsByTypes(Long recipientId, LocalDateTime lastCreatedAt, Long lastId, int size, List<String> types);
    
    /**
     * 获取用户指定分组的未读通知数量
     */
    Long countUnreadByGroup(Long recipientId, String notificationGroup);
}
