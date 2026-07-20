package com.microsoft.aediumbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.microsoft.aediumbackend.model.dto.user.response.UserBriefDTO;
import com.microsoft.aediumbackend.model.entity.User;
import org.apache.ibatis.annotations.MapKey;

import java.util.List;
import java.util.Set;

public interface UserMapper extends BaseMapper<User> {


    List<UserBriefDTO> getUsersBriefByIds(Set<Long> userIds);
}