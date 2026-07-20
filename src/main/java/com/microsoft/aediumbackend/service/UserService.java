package com.microsoft.aediumbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.microsoft.aediumbackend.model.dto.user.request.UserUpdateRequest;
import com.microsoft.aediumbackend.model.dto.user.response.UserBriefDTO;
import com.microsoft.aediumbackend.model.entity.User;
import com.microsoft.aediumbackend.model.vo.UserVO;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface UserService extends IService<User> {

    /**
     * 用户注册
     */
    Long userRegister(String userAccount, String password, String checkPassword);

    /**
     * 用户登录
     */
    UserVO userLogin(String userAccount, String password, HttpServletResponse response);

    /**
     * 获取集合的用户VO
     */
    List<UserVO> getUserVO(List<User> userList);

    /**
     * 校验用户信息
     */
    void validateUserInfo(String username, String email);

    /**
     * 校验更新信息
     */
    void validateUserUpdate(UserUpdateRequest request);

    /**
     * 校验密码
     */
    void validatePassword(String password);

    Map<Long, UserBriefDTO> getUsersBriefByIds(Set<Long> userIds);
}
