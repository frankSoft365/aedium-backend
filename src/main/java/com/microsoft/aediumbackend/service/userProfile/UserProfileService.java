package com.microsoft.aediumbackend.service.userProfile;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.microsoft.aediumbackend.mapper.UserProfileMapper;
import com.microsoft.aediumbackend.model.entity.UserProfile;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService extends ServiceImpl<UserProfileMapper, UserProfile> {
}
