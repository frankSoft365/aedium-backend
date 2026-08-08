package com.microsoft.aediumbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.microsoft.aediumbackend.model.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 根据通知类型列表查询通知
     */
    List<Notification> findNotificationsByTypes(@Param("recipientId") Long recipientId,
                                                 @Param("lastCreatedAt") LocalDateTime lastCreatedAt,
                                                 @Param("lastId") Long lastId,
                                                 @Param("size") int size,
                                                 @Param("types") List<String> types);

    /**
     * 获取用户指定分组的未读通知数量
     */
    Long countUnreadByGroup(@Param("recipientId") Long recipientId,
                            @Param("notificationGroup") String notificationGroup,
                            @Param("types") List<String> types);
}
