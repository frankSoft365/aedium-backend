package com.microsoft.aediumbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.microsoft.aediumbackend.model.entity.Comment;
import com.microsoft.aediumbackend.model.entity.UserReadingHistory;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserReadingHistoryMapper extends BaseMapper<UserReadingHistory> {
    void insertOrUpdateReadHistory(
            @Param("userId") Long userId,
            @Param("articleId") Long articleId
    );

    List<UserReadingHistory> findUserReadingHistoryCursor(
            @Param("userId") Long userId,
            @Param("lastReadingTime") LocalDateTime lastReadingTime,
            @Param("lastId") Long lastId,
            @Param("size") Integer size
    );
}
