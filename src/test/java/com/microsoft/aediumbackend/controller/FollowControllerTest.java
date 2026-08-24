package com.microsoft.aediumbackend.controller;

import com.microsoft.aediumbackend.commen.CursorPage;
import com.microsoft.aediumbackend.exception.GlobalExceptionHandler;
import com.microsoft.aediumbackend.model.dto.follow.FollowBatchStatusResult;
import com.microsoft.aediumbackend.model.vo.UserVO;
import com.microsoft.aediumbackend.service.UserFollowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("用户关注接口测试")
class FollowControllerTest {

    @Mock
    private UserFollowService userFollowService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FollowController(userFollowService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("批量查询关注状态成功")
    void batchStatus_success() throws Exception {
        when(userFollowService.batchStatus(any()))
                .thenReturn(new FollowBatchStatusResult(Map.of(2L, true, 3L, false)));

        mockMvc.perform(post("/follow/batch-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetUserIds": ["2", "3"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.followingMap['2']").value(true))
                .andExpect(jsonPath("$.data.followingMap['3']").value(false));

        verify(userFollowService).batchStatus(any());
    }

    @Test
    @DisplayName("目标用户列表为空时返回参数错误")
    void batchStatus_emptyTargetUserIds_returnsParamError() throws Exception {
        mockMvc.perform(post("/follow/batch-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetUserIds": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400000))
                .andExpect(jsonPath("$.description")
                        .value("targetUserIds: 目标用户ID列表不能为空"));

        verify(userFollowService, never()).batchStatus(any());
    }

    @Test
    @DisplayName("关注操作成功")
    void handleAction_success() throws Exception {
        mockMvc.perform(post("/follow/action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetUserId": "2",
                                  "action": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(userFollowService).handleAction(any());
    }

    @Test
    @DisplayName("关注操作类型为空时返回参数错误")
    void handleAction_emptyAction_returnsParamError() throws Exception {
        mockMvc.perform(post("/follow/action")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetUserId": "2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400000))
                .andExpect(jsonPath("$.description")
                        .value("action: 关注操作类型不合法"));

        verify(userFollowService, never()).handleAction(any());
    }

    @Test
    @DisplayName("查询粉丝列表成功")
    void getFollowers_success() throws Exception {
        LocalDateTime followTime = LocalDateTime.of(2026, 8, 24, 12, 0);
        UserVO follower = new UserVO();
        follower.setId(2L);
        follower.setUsername("follower");
        when(userFollowService.getFollowers(eq(1L), any())).thenReturn(
                new CursorPage<>(List.of(follower), false, followTime, 2L)
        );

        mockMvc.perform(get("/follow/followers")
                        .param("userId", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items[0].id").value(2))
                .andExpect(jsonPath("$.data.items[0].username").value("follower"))
                .andExpect(jsonPath("$.data.hasMore").value(false))
                .andExpect(jsonPath("$.data.nextCursorId").value(2));

        verify(userFollowService).getFollowers(
                eq(1L),
                argThat(request -> request.getSize() == 20
                        && request.getLastCreatedAt() == null
                        && request.getLastId() == null)
        );
    }

    @Test
    @DisplayName("查询关注列表成功")
    void getFollowing_success() throws Exception {
        when(userFollowService.getFollowing(eq(1L), any())).thenReturn(
                new CursorPage<>(List.of(), false, null, null)
        );

        mockMvc.perform(get("/follow/following")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.hasMore").value(false));

        verify(userFollowService).getFollowing(
                eq(1L),
                argThat(request -> request.getSize() == 12)
        );
    }
}
