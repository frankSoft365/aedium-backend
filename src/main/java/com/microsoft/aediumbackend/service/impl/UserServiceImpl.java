package com.microsoft.aediumbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.exception.BusinessException;
import com.microsoft.aediumbackend.mapper.UserMapper;
import com.microsoft.aediumbackend.model.dto.follow.response.SocialStats;
import com.microsoft.aediumbackend.model.dto.user.request.UserUpdateRequest;
import com.microsoft.aediumbackend.model.dto.user.response.Membership;
import com.microsoft.aediumbackend.model.dto.user.response.UserBriefDTO;
import com.microsoft.aediumbackend.model.dto.user.response.UserInfo;
import com.microsoft.aediumbackend.model.entity.User;
import com.microsoft.aediumbackend.model.entity.UserProfile;
import com.microsoft.aediumbackend.model.entity.UserWithProfile;
import com.microsoft.aediumbackend.model.vo.UserVO;
import com.microsoft.aediumbackend.service.CollectionListService;
import com.microsoft.aediumbackend.service.UserService;
import com.microsoft.aediumbackend.service.userProfile.UserProfileService;
import com.microsoft.aediumbackend.utils.JwtUtils;
import com.microsoft.aediumbackend.utils.PasswordUtils;
import com.microsoft.aediumbackend.utils.RegexUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.microsoft.aediumbackend.constant.CommonConstant.*;
import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.*;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final int MIN_USER_ACCOUNT_LENGTH = 6;
    private static final int MAX_USER_ACCOUNT_LENGTH = 20;
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_LENGTH = 20;

    @Value("${secure.cookie}")
    private boolean secureCookie;

    @Value("${secure.same-site}")
    private String cookieSameSite;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private UserMapper userMapper;
    @Resource
    private CollectionListService collectionListService;
    @Autowired
    private UserProfileService userProfileService;

    /**
     * 用户注册
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long userRegister(String username, String email, String password) {
        if (StringUtils.isAnyBlank(username, email, password)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, CREDENTIAL_INCOMPLETE);
        }
        // 账户名的长度要求 6-20 密码的长度要求 6-20
        if (username.length() < MIN_USER_ACCOUNT_LENGTH || username.length() > MAX_USER_ACCOUNT_LENGTH) {
            // 参数不合法
            throw new BusinessException(ErrorCode.PARAM_ERROR, USER_ACCOUNT_LENGTH_INVALID);
        }
        if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PASSWORD_LENGTH_INVALID);
        }
        if (!RegexUtils.isValidUserAccount(username)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, USER_ACCOUNT_FORMAT_INVALID);
        }
        if (!RegexUtils.isValidEmail(email)) {
            // 参数不合法
            throw new BusinessException(ErrorCode.PARAM_ERROR, PASSWORD_FORMAT_INVALID);
        }
        if (!RegexUtils.isValidPassword(password)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PASSWORD_FORMAT_INVALID);
        }
        // 邮箱不能有重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);
        User existUser = this.getOne(queryWrapper);
        if (existUser != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, EMAIL_DUPLICATE);
        }
        // 将密码加密
        String encodePassword = PasswordUtils.encodePassword(password);
        // 插入用户数据
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(encodePassword);
        boolean result = this.save(user);
        if (!result) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, DATABASE_INSERT_FAILED);
        }
        Long userId = user.getId();

        // 创建默认收藏夹
        collectionListService.createDefaultList(userId);
        // 创建用户扩展表
        UserProfile userProfile = new UserProfile();
        userProfile.setUserId(userId);
        userProfileService.save(userProfile);

        log.info("用户注册成功");
        return userId;
    }

    /**
     * 用户登录
     */
    @Override
    public UserVO userLogin(String email, String password, HttpServletResponse response) {
        if (StringUtils.isAnyBlank(email, password)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, CREDENTIAL_INCOMPLETE);
        }
        if (password.length() < 6 || password.length() > 20) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PASSWORD_LENGTH_INVALID);
        }
        if (!RegexUtils.isValidEmail(email)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, USER_ACCOUNT_FORMAT_INVALID);
        }
        if (!RegexUtils.isValidPassword(password)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PASSWORD_FORMAT_INVALID);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);
        User user = this.getOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, USER_ACCOUNT_OR_PASSWORD_INCORRECT);
        }
        // 如果存在 将将要校验的密码与数据库的密码比对看是否一致
        String md5Hex = PasswordUtils.encodePassword(password);
        if (!md5Hex.equals(user.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, USER_ACCOUNT_OR_PASSWORD_INCORRECT);
        }
        // 如果一致 登录成功 发放token
        long currentTimestamp = System.currentTimeMillis();
        Long userId = user.getId();

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put(TOKEN_PAYLOAD_KEY_1, userId);
        dataMap.put(TOKEN_PAYLOAD_KEY_3, currentTimestamp);
        String token = JwtUtils.generateToken(dataMap);

        stringRedisTemplate.opsForValue().set("user:valid_time:" + userId, String.valueOf(currentTimestamp), 7, TimeUnit.DAYS);

        ResponseCookie cookie = ResponseCookie.from(TOKEN_COOKIE_FIELD, token)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        log.info("用户登录成功");
        return UserVO.getUserVO(user);
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (userList.isEmpty()) {
            return new ArrayList<>();
        }
        return userList.stream().map(UserVO::getUserVO).toList();
    }

    @Override
    public void validateUserInfo(String username, String email) {
        if (StringUtils.isAnyBlank(username, email)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, USERNAME_EMPTY);
        }
        if (username.length() < MIN_USER_ACCOUNT_LENGTH || username.length() > MAX_USER_ACCOUNT_LENGTH) {
            // 参数不合法
            throw new BusinessException(ErrorCode.PARAM_ERROR, USER_ACCOUNT_LENGTH_INVALID);
        }
        if (!RegexUtils.isValidUserAccount(username)) {
            // 参数不合法
            throw new BusinessException(ErrorCode.PARAM_ERROR, USER_ACCOUNT_FORMAT_INVALID);
        }
        if (!RegexUtils.isValidEmail(email)) {
            // 参数不合法
            throw new BusinessException(ErrorCode.PARAM_ERROR, PASSWORD_FORMAT_INVALID);
        }
    }

    @Override
    public void validateUserUpdate(UserUpdateRequest request) {
        String username = request.getUsername();
        String image = request.getImage();
        if (!StringUtils.isBlank(username)) {
            if (username.length() < MIN_USER_ACCOUNT_LENGTH || username.length() > MAX_USER_ACCOUNT_LENGTH) {
                // 参数不合法
                throw new BusinessException(ErrorCode.PARAM_ERROR, USER_ACCOUNT_LENGTH_INVALID);
            }
            if (!RegexUtils.isValidUserAccount(username)) {
                // 参数不合法
                throw new BusinessException(ErrorCode.PARAM_ERROR, USER_ACCOUNT_FORMAT_INVALID);
            }
        }
    }

    @Override
    public void validatePassword(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PASSWORD_LENGTH_INVALID);
        }
        if (!RegexUtils.isValidPassword(password)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PASSWORD_FORMAT_INVALID);
        }
    }

    @Override
    public Map<Long, UserBriefDTO> getUsersBriefByIds(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        List<UserBriefDTO> list = userMapper.getUsersBriefByIds(userIds);
        return list.stream().collect(Collectors.toMap(UserBriefDTO::getId, dto -> dto));
    }

    @Override
    public UserInfo getUserInfoByUserId(Long userId) {
        // 冗余校验代码
        LambdaQueryWrapper<UserProfile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserProfile::getUserId, userId);
        UserProfile userProfile = userProfileService.getOne(queryWrapper);
        if (userProfile == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, USER_NOT_FOUND);
        }

        UserWithProfile user = userMapper.getUserInfoById(userId);
        UserInfo userInfo = new UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        SocialStats socialStats = new SocialStats();
        socialStats.setUserId(userId);
        socialStats.setFollowerCount(user.getFollowerCount());
        socialStats.setFollowingCount(user.getFollowingCount());
        userInfo.setSocialStats(socialStats);
        Membership membership = new Membership();
        membership.setMemberSince(user.getCreateTime());
        userInfo.setMembership(membership);

        boolean isBioEmpty = user.getBio() == null || user.getBio().isBlank();
        userInfo.setBio(isBioEmpty ? "" : user.getBio());

        userInfo.setAvatar(user.getImage());

        boolean isAboutEmpty = user.getAbout() == null || user.getAbout().isBlank();
        userInfo.setAbout(isAboutEmpty ? "" : user.getAbout());
        return userInfo;
    }

    @Override
    public void updateUserProfileAbout(String about, Long userId) {
        LambdaUpdateWrapper<UserProfile> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserProfile::getUserId, userId);
        updateWrapper.set(UserProfile::getAbout, about);
        boolean update = userProfileService.update(updateWrapper);
        if (!update) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, USER_NOT_FOUND);
        }
    }

}
