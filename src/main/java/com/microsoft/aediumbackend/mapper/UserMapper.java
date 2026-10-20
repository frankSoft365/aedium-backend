package com.microsoft.aediumbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.microsoft.aediumbackend.model.dto.user.response.UserBriefDTO;
import com.microsoft.aediumbackend.model.entity.User;
import com.microsoft.aediumbackend.model.entity.UserWithProfile;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

public interface UserMapper extends BaseMapper<User> {

    List<UserBriefDTO> getUsersBriefByIds(Set<Long> userIds);

    @Select("select u.id, username, image, u.create_time, follower_count, " +
            "following_count, up.bio, up.about from user u join user_profile up on u.id=up.user_id where u.id=#{userId} and u.is_delete=0 and up.is_delete=0")
    UserWithProfile getUserInfoById(Long userId);
}