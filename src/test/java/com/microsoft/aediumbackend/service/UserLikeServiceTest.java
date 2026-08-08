package com.microsoft.aediumbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.exception.BusinessException;
import com.microsoft.aediumbackend.mapper.ArticleMapper;
import com.microsoft.aediumbackend.mapper.CommentMapper;
import com.microsoft.aediumbackend.mapper.UserLikeMapper;
import com.microsoft.aediumbackend.model.dto.like.LikeActionRequest;
import com.microsoft.aediumbackend.model.dto.like.LikeBatchStatusRequest;
import com.microsoft.aediumbackend.model.dto.like.LikeBatchStatusResult;
import com.microsoft.aediumbackend.model.entity.Article;
import com.microsoft.aediumbackend.model.entity.Comment;
import com.microsoft.aediumbackend.model.entity.UserLike;
import com.microsoft.aediumbackend.model.enums.NotificationTargetType;
import com.microsoft.aediumbackend.model.enums.NotificationType;
import com.microsoft.aediumbackend.service.impl.like.LikePushService;
import com.microsoft.aediumbackend.utils.CurrentHold;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("点赞系统单元测试")
class UserLikeServiceTest {

    @InjectMocks
    private UserLikeService userLikeService;

    @Mock
    private UserLikeMapper userLikeMapper;
    @Mock
    private ArticleMapper articleMapper;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private LikePushService likePushService;

    private static final Long USER_ID = 1L;
    private static final Long ARTICLE_ID = 100L;
    private static final Long ARTICLE_AUTHOR_ID = 2L;
    private static final Long COMMENT_ID = 200L;
    private static final Long COMMENT_USER_ID = 3L;

    @BeforeEach
    void setUp() {
        CurrentHold.setCurrentId(USER_ID);
    }

    @AfterEach
    void tearDown() {
        CurrentHold.removeId();
    }

    // ==================== handleAction 参数校验 ====================

    @Test
    @DisplayName("未登录 - 抛出异常")
    void handleAction_notLoggedIn_throws() {
        CurrentHold.removeId();
        LikeActionRequest req = new LikeActionRequest();
        req.setTargetType(1);
        req.setTargetId(ARTICLE_ID);
        req.setAction(1);

        BusinessException ex = assertThrows(BusinessException.class, () -> userLikeService.handleAction(req));
        assertEquals(ErrorCode.NO_AUTH.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("参数为空 - 抛出异常")
    void handleAction_nullParams_throws() {
        LikeActionRequest req = new LikeActionRequest();
        req.setTargetType(1);
        req.setTargetId(ARTICLE_ID);
        req.setAction(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> userLikeService.handleAction(req));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("非法 targetType - 抛出异常")
    void handleAction_invalidTargetType_throws() {
        LikeActionRequest req = new LikeActionRequest();
        req.setTargetType(99);
        req.setTargetId(ARTICLE_ID);
        req.setAction(1);

        BusinessException ex = assertThrows(BusinessException.class, () -> userLikeService.handleAction(req));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("非法 action - 抛出异常")
    void handleAction_invalidAction_throws() {
        LikeActionRequest req = new LikeActionRequest();
        req.setTargetType(1);
        req.setTargetId(ARTICLE_ID);
        req.setAction(99);

        BusinessException ex = assertThrows(BusinessException.class, () -> userLikeService.handleAction(req));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    // ==================== 文章点赞流程 ====================

    @Test
    @DisplayName("首次点赞文章 - 插入记录 + 计数+1 + 创建通知")
    void likeArticle_firstTime_insertsAndNotifies() {
        LikeActionRequest req = new LikeActionRequest();
        req.setTargetType(1);
        req.setTargetId(ARTICLE_ID);
        req.setAction(1);

        Article article = new Article();
        article.setId(ARTICLE_ID);
        article.setAuthorId(ARTICLE_AUTHOR_ID);

        when(userLikeMapper.findByUserAndTarget(USER_ID, 1, ARTICLE_ID)).thenReturn(null);
        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(article);
        when(notificationService.count(any(LambdaQueryWrapper.class))).thenReturn(0L);

        userLikeService.handleAction(req);

        verify(userLikeMapper).insert(any(UserLike.class));
        verify(articleMapper).incrementLikeCount(ARTICLE_ID);
        verify(notificationService).count(any(LambdaQueryWrapper.class));
        verify(notificationService).createNotification(
                eq(ARTICLE_AUTHOR_ID), eq(USER_ID),
                eq(NotificationType.LIKE_ARTICLE.getValue()),
                eq(NotificationTargetType.ARTICLE.getValue()),
                eq(ARTICLE_ID), isNull()
        );
    }

    @Test
    @DisplayName("重复点赞文章（已点赞） - 不做任何操作")
    void likeArticle_alreadyLiked_noop() {
        LikeActionRequest req = new LikeActionRequest();
        req.setTargetType(1);
        req.setTargetId(ARTICLE_ID);
        req.setAction(1);

        Article article = new Article();
        article.setId(ARTICLE_ID);
        article.setAuthorId(ARTICLE_AUTHOR_ID);

        UserLike existing = new UserLike();
        existing.setId(10L);
        existing.setIsDeleted(0);

        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(article);
        when(userLikeMapper.findByUserAndTarget(USER_ID, 1, ARTICLE_ID)).thenReturn(existing);

        userLikeService.handleAction(req);

        verify(userLikeMapper, never()).insert(any(UserLike.class));
        verify(userLikeMapper, never()).restoreLike(anyLong());
        verify(articleMapper, never()).incrementLikeCount(anyLong());
        verify(notificationService, never()).createNotification(anyLong(), anyLong(), any(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("重新点赞文章（已取消） - 恢复记录 + 计数+1 + 去重通知")
    void likeArticle_afterUnlike_restoresAndNotifies() {
        LikeActionRequest req = new LikeActionRequest();
        req.setTargetType(1);
        req.setTargetId(ARTICLE_ID);
        req.setAction(1);

        UserLike existing = new UserLike();
        existing.setId(10L);
        existing.setIsDeleted(1);

        Article article = new Article();
        article.setId(ARTICLE_ID);
        article.setAuthorId(ARTICLE_AUTHOR_ID);

        when(userLikeMapper.findByUserAndTarget(USER_ID, 1, ARTICLE_ID)).thenReturn(existing);
        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(article);
        when(notificationService.count(any(LambdaQueryWrapper.class))).thenReturn(1L);

        userLikeService.handleAction(req);

        verify(userLikeMapper).restoreLike(10L);
        verify(articleMapper).incrementLikeCount(ARTICLE_ID);
        verify(notificationService).count(any(LambdaQueryWrapper.class));
        verify(notificationService, never()).createNotification(anyLong(), anyLong(), any(), any(), anyLong(), any());
    }

    // ==================== 文章取消点赞流程 ====================

    @Test
    @DisplayName("取消点赞文章 - 标记删除 + 计数-1")
    void unlikeArticle_liked_cancelsAndDecrements() {
        LikeActionRequest req = new LikeActionRequest();
        req.setTargetType(1);
        req.setTargetId(ARTICLE_ID);
        req.setAction(2);

        UserLike existing = new UserLike();
        existing.setId(10L);
        existing.setIsDeleted(0);

        when(userLikeMapper.findByUserAndTarget(USER_ID, 1, ARTICLE_ID)).thenReturn(existing);

        userLikeService.handleAction(req);

        verify(userLikeMapper).cancelLike(10L);
        verify(articleMapper).decrementLikeCount(ARTICLE_ID);
        verify(notificationService, never()).createNotification(anyLong(), anyLong(), any(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("重复取消文章点赞（已取消） - 不做任何操作")
    void unlikeArticle_alreadyCancelled_noop() {
        LikeActionRequest req = new LikeActionRequest();
        req.setTargetType(1);
        req.setTargetId(ARTICLE_ID);
        req.setAction(2);

        UserLike existing = new UserLike();
        existing.setId(10L);
        existing.setIsDeleted(1);

        when(userLikeMapper.findByUserAndTarget(USER_ID, 1, ARTICLE_ID)).thenReturn(existing);

        userLikeService.handleAction(req);

        verify(userLikeMapper, never()).cancelLike(anyLong());
        verify(articleMapper, never()).decrementLikeCount(anyLong());
    }

    @Test
    @DisplayName("取消从未点赞的文章 - 不做任何操作")
    void unlikeArticle_neverLiked_noop() {
        LikeActionRequest req = new LikeActionRequest();
        req.setTargetType(1);
        req.setTargetId(ARTICLE_ID);
        req.setAction(2);

        when(userLikeMapper.findByUserAndTarget(USER_ID, 1, ARTICLE_ID)).thenReturn(null);

        userLikeService.handleAction(req);

        verify(userLikeMapper, never()).cancelLike(anyLong());
        verify(articleMapper, never()).decrementLikeCount(anyLong());
    }

    // ==================== 评论点赞流程 ====================

    @Test
    @DisplayName("首次点赞评论 - 插入记录 + 计数+1 + 创建通知(含articleId)")
    void likeComment_firstTime_insertsAndNotifies() {
        LikeActionRequest req = new LikeActionRequest();
        req.setTargetType(2);
        req.setTargetId(COMMENT_ID);
        req.setAction(1);

        Comment comment = new Comment();
        comment.setId(COMMENT_ID);
        comment.setUserId(COMMENT_USER_ID);
        comment.setArticleId(ARTICLE_ID);

        when(userLikeMapper.findByUserAndTarget(USER_ID, 2, COMMENT_ID)).thenReturn(null);
        when(commentMapper.selectById(COMMENT_ID)).thenReturn(comment);
        when(notificationService.count(any(LambdaQueryWrapper.class))).thenReturn(0L);

        userLikeService.handleAction(req);

        verify(userLikeMapper).insert(any(UserLike.class));
        verify(commentMapper).incrementLikeCount(COMMENT_ID);
        verify(notificationService).createNotification(
                eq(COMMENT_USER_ID), eq(USER_ID),
                eq(NotificationType.LIKE_COMMENT.getValue()),
                eq(NotificationTargetType.COMMENT.getValue()),
                eq(COMMENT_ID), eq(Map.of("articleId", ARTICLE_ID))
        );
    }

    @Test
    @DisplayName("取消评论点赞 - 标记删除 + 计数-1")
    void unlikeComment_liked_cancelsAndDecrements() {
        LikeActionRequest req = new LikeActionRequest();
        req.setTargetType(2);
        req.setTargetId(COMMENT_ID);
        req.setAction(2);

        UserLike existing = new UserLike();
        existing.setId(20L);
        existing.setIsDeleted(0);

        when(userLikeMapper.findByUserAndTarget(USER_ID, 2, COMMENT_ID)).thenReturn(existing);

        userLikeService.handleAction(req);

        verify(userLikeMapper).cancelLike(20L);
        verify(commentMapper).decrementLikeCount(COMMENT_ID);
    }

    // ==================== 通知去重 ====================

    @Test
    @DisplayName("点赞自己的文章 - 抛出异常")
    void likeOwnArticle_throws() {
        LikeActionRequest req = new LikeActionRequest();
        req.setTargetType(1);
        req.setTargetId(ARTICLE_ID);
        req.setAction(1);

        Article article = new Article();
        article.setId(ARTICLE_ID);
        article.setAuthorId(USER_ID); // 自己的文章

        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(article);

        BusinessException ex = assertThrows(BusinessException.class, () -> userLikeService.handleAction(req));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());

        verify(userLikeMapper, never()).insert(any(UserLike.class));
        verify(articleMapper, never()).incrementLikeCount(anyLong());
        verify(notificationService, never()).createNotification(anyLong(), anyLong(), any(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("点赞不存在的文章 - 抛出异常")
    void likeNonExistentArticle_throws() {
        LikeActionRequest req = new LikeActionRequest();
        req.setTargetType(1);
        req.setTargetId(999L);
        req.setAction(1);

        when(articleMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> userLikeService.handleAction(req));
        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), ex.getCode());

        verify(userLikeMapper, never()).insert(any(UserLike.class));
        verify(articleMapper, never()).incrementLikeCount(anyLong());
        verify(notificationService, never()).createNotification(anyLong(), anyLong(), any(), any(), anyLong(), any());
    }

    // ==================== batchStatus ====================

    @Test
    @DisplayName("批量查询点赞状态 - 返回正确结果")
    void batchStatus_returnsCorrectMap() {
        LikeBatchStatusRequest req = new LikeBatchStatusRequest();
        req.setTargetType(1);
        req.setTargetIds(List.of(1L, 2L, 3L));

        when(userLikeMapper.findLikedTargetIds(USER_ID, 1, List.of(1L, 2L, 3L)))
                .thenReturn(List.of(1L, 3L));

        LikeBatchStatusResult result = userLikeService.batchStatus(req);

        assertNotNull(result);
        assertEquals(true, result.getLikedMap().get("1"));
        assertEquals(false, result.getLikedMap().get("2"));
        assertEquals(true, result.getLikedMap().get("3"));
    }

    @Test
    @DisplayName("批量查询 - 未登录抛出异常")
    void batchStatus_notLoggedIn_throws() {
        CurrentHold.removeId();
        LikeBatchStatusRequest req = new LikeBatchStatusRequest();
        req.setTargetType(1);
        req.setTargetIds(List.of(1L));

        BusinessException ex = assertThrows(BusinessException.class, () -> userLikeService.batchStatus(req));
        assertEquals(ErrorCode.NO_AUTH.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("批量查询 - 空ID列表抛出异常")
    void batchStatus_emptyIds_throws() {
        LikeBatchStatusRequest req = new LikeBatchStatusRequest();
        req.setTargetType(1);
        req.setTargetIds(List.of());

        BusinessException ex = assertThrows(BusinessException.class, () -> userLikeService.batchStatus(req));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("批量查询 - 非法targetType抛出异常")
    void batchStatus_invalidTargetType_throws() {
        LikeBatchStatusRequest req = new LikeBatchStatusRequest();
        req.setTargetType(99);
        req.setTargetIds(List.of(1L));

        BusinessException ex = assertThrows(BusinessException.class, () -> userLikeService.batchStatus(req));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }
}