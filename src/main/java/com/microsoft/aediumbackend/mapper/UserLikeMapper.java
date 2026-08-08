package com.microsoft.aediumbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.microsoft.aediumbackend.model.entity.UserLike;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface UserLikeMapper extends BaseMapper<UserLike> {

    @Select("SELECT id, user_id, target_type, target_id, is_deleted, create_time, update_time " +
            "FROM user_like " +
            "WHERE user_id = #{userId} " +
            "AND target_type = #{targetType} " +
            "AND target_id = #{targetId} " +
            "LIMIT 1")
    UserLike findByUserAndTarget(@Param("userId") Long userId,
                                @Param("targetType") Integer targetType,
                                @Param("targetId") Long targetId);

    @Update("UPDATE user_like SET is_deleted = 0 WHERE id = #{id}")
    int restoreLike(@Param("id") Long id);

    @Update("UPDATE user_like SET is_deleted = 1 WHERE id = #{id} AND is_deleted = 0")
    int cancelLike(@Param("id") Long id);

    List<Long> findLikedTargetIds(@Param("userId") Long userId,
                                  @Param("targetType") Integer targetType,
                                  @Param("targetIds") List<Long> targetIds);
}