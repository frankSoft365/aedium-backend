package com.microsoft.aediumbackend.model.vo;

import com.microsoft.aediumbackend.model.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("用户视图转换测试")
class UserVOTest {

    @Test
    @DisplayName("包含粉丝数和关注数")
    void getUserVO_includesFollowCounts() {
        User user = new User();
        user.setFollowerCount(12);
        user.setFollowingCount(34);

        UserVO userVO = UserVO.getUserVO(user);

        assertEquals(12, userVO.getFollowerCount());
        assertEquals(34, userVO.getFollowingCount());
    }
}
