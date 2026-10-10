package com.microsoft.aediumbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.microsoft.aediumbackend.model.dto.repost.response.RepostCount;
import com.microsoft.aediumbackend.model.entity.UserRepost;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserRepostMapper extends BaseMapper<UserRepost> {
    List<RepostCount> batchQueryRepostCountByArticleIds(@Param("articleIds") List<Long> articleIds);

    List<UserRepost> findUserRepostCursor(
            @Param("userId") Long userId,
            @Param("lastCreateTime") LocalDateTime lastCreateTime,
            @Param("lastId") Long lastId,
            @Param("size") Integer size
    );
}
