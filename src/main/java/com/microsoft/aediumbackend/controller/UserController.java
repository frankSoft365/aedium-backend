package com.microsoft.aediumbackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.commen.Result;
import com.microsoft.aediumbackend.exception.BusinessException;
import com.microsoft.aediumbackend.model.dto.user.UserChangePasswordRequest;
import com.microsoft.aediumbackend.model.dto.user.UserLoginRequest;
import com.microsoft.aediumbackend.model.dto.user.UserRegisterRequest;
import com.microsoft.aediumbackend.model.dto.user.UserUpdateRequest;
import com.microsoft.aediumbackend.model.entity.User;
import com.microsoft.aediumbackend.model.vo.UserVO;
import com.microsoft.aediumbackend.service.UserService;
import com.microsoft.aediumbackend.service.impl.EmailVerifyServiceImpl;
import com.microsoft.aediumbackend.utils.CurrentHold;
import com.microsoft.aediumbackend.utils.PasswordUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.microsoft.aediumbackend.constant.CommonConstant.TOKEN_COOKIE_FIELD;
import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.*;


@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${secure.cookie}")
    private boolean secureCookie;

    @Value("${secure.same-site}")
    private String cookieSameSite;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_EMPTY);
        }
        String token = userRegisterRequest.getToken();
        String username = userRegisterRequest.getUsername();
        String password = userRegisterRequest.getPassword();

        if (StringUtils.isAnyBlank(username, token, password)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, CREDENTIAL_INCOMPLETE);
        }

        String key = EmailVerifyServiceImpl.getTokenEmailKey(token);
        String email = stringRedisTemplate.opsForValue().get(key);
        if (email == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, VERIFICATION_TIMED_OUT);
        }

        Long userId = userService.userRegister(username, email, password);
        if (userId > 0) {
            stringRedisTemplate.delete(key);
        }
        return Result.success(userId);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<UserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletResponse response) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_EMPTY);
        }
        String email = userLoginRequest.getEmail();
        String password = userLoginRequest.getPassword();
        if (StringUtils.isAllBlank(email, password)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, LOGIN_CREDENTIAL_EMPTY);
        }
        UserVO userVO = userService.userLogin(email, password, response);
        return Result.success(userVO);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result<Void> userLogout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(TOKEN_COOKIE_FIELD, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(0)
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return Result.success();
    }

    /**
     * 用户修改密码
     */
    @PostMapping("/changePassword")
    public Result<Void> userChangePassword(@RequestBody UserChangePasswordRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_EMPTY);
        }
        String currentPassword = request.getCurrentPassword();
        String newPassword = request.getNewPassword();
        userService.validatePassword(currentPassword);
        userService.validatePassword(newPassword);

        if (currentPassword.equals(newPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, OLD_NEW_PASSWORD_SAME);
        }

        Long userId = CurrentHold.getCurrentId();
        User userInfo = userService.getById(userId);
        if (userInfo == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, OBJECT_NOT_FOUND);
        }
        String originalPassword = userInfo.getPassword();
        if (StringUtils.isBlank(originalPassword)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, PASSWORD_EMPTY);
        }
        String encodedCurrentPassword = PasswordUtils.encodePassword(currentPassword);
        if (!encodedCurrentPassword.equals(originalPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, CURRENT_PASSWORD_INCORRECT);
        }

        String encodedNewPassword = PasswordUtils.encodePassword(newPassword);

        User user = new User();
        user.setId(userId);
        user.setPassword(encodedNewPassword);
        boolean updateById = userService.updateById(user);
        if (!updateById) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, DATABASE_UPDATE_FAILED);
        }

        long newTokenTime = System.currentTimeMillis();
        stringRedisTemplate.opsForValue().set("user:valid_time:" + userId, String.valueOf(newTokenTime), 7, TimeUnit.DAYS);

        return Result.success();
    }

    /**
     * 用户编辑个人信息
     */
    @PostMapping("/update")
    public Result<Void> updateUserInfo(@RequestBody UserUpdateRequest userInfoToUpdate) {
        if (userInfoToUpdate == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_EMPTY);
        }
        userService.validateUserUpdate(userInfoToUpdate);
        User user = new User();
        BeanUtils.copyProperties(userInfoToUpdate, user);
        Long currentId = CurrentHold.getCurrentId();
        user.setId(currentId);
        boolean update = userService.updateById(user);
        if (!update) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, DATABASE_UPDATE_FAILED);
        }
        return Result.success();
    }

    /**
     * 根据用户名模糊查询用户列表
     */
    @GetMapping("/search")
    public Result<List<UserVO>> searchUsers(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (!StringUtils.isAllBlank(username)) {
            queryWrapper.like("username", username);
        }
        // 用户信息脱敏
        List<User> list = userService.list(queryWrapper);
        List<UserVO> userVOList = userService.getUserVO(list);
        return Result.success(userVOList);
    }

    /**
     * 根据用户的登录态获取用户信息
     */
    @GetMapping("/current")
    public Result<UserVO> getCurrentUser() {
        Long currentId = CurrentHold.getCurrentId();
        User user = userService.getById(currentId);
        UserVO userVO = UserVO.getUserVO(user);
        return Result.success(userVO);
    }

    /**
     * 删除用户 只有管理员可以发起删除请求
     */
    @PostMapping("/delete")
    public Result<Void> deleteUser(@RequestBody Long id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_FORMAT_ERROR);
        }
        userService.removeById(id);
        return Result.success();
    }
}
