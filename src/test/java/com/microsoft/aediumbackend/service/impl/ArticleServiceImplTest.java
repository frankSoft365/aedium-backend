package com.microsoft.aediumbackend.service.impl;

import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.exception.BusinessException;
import com.microsoft.aediumbackend.mapper.ArticleMapper;
import com.microsoft.aediumbackend.mapper.ArticleTopicMapper;
import com.microsoft.aediumbackend.mapper.TopicMapper;
import com.microsoft.aediumbackend.model.dto.article.request.ArticleListRequest;
import com.microsoft.aediumbackend.service.ArticleTopicService;
import com.microsoft.aediumbackend.service.TopicService;
import com.microsoft.aediumbackend.service.impl.comment.CommentCountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleServiceImplTest {

    @Mock
    private TopicService topicService;
    @Mock
    private ArticleTopicService articleTopicService;
    @Mock
    private TopicMapper topicMapper;
    @Mock
    private ArticleMapper articleMapper;
    @Mock
    private ArticleTopicMapper articleTopicMapper;
    @Mock
    private CommentCountService commentCountService;

    @InjectMocks
    private ArticleServiceImpl articleService;

    @Test
    @DisplayName("公共文章列表为空时不查询评论数")
    void getArticleList_emptyResult_returnsEmptyList() {
        ArticleListRequest request = new ArticleListRequest();
        request.setIsMyArticle(false);
        when(articleMapper.getArticleList()).thenReturn(List.of());

        assertTrue(articleService.getArticleList(request).isEmpty());

        verifyNoInteractions(commentCountService);
    }

    @Test
    @DisplayName("用户文章列表为空时不查询评论数")
    void getArticleList_emptyUserResult_returnsEmptyList() {
        ArticleListRequest request = new ArticleListRequest();
        request.setIsMyArticle(true);
        request.setUserId(1L);
        when(articleMapper.getUserArticleList(1L)).thenReturn(List.of());

        assertTrue(articleService.getArticleList(request).isEmpty());

        verifyNoInteractions(commentCountService);
    }

    @Test
    @DisplayName("未传isMyArticle时按公共文章列表查询")
    void getArticleList_withoutType_queriesPublicArticles() {
        ArticleListRequest request = new ArticleListRequest();
        when(articleMapper.getArticleList()).thenReturn(List.of());

        assertTrue(articleService.getArticleList(request).isEmpty());

        verify(articleMapper).getArticleList();
        verify(articleMapper, never()).getUserArticleList(any());
    }

    @Test
    @DisplayName("文章列表请求为空时返回参数错误")
    void getArticleList_nullRequest_throwsParamError() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> articleService.getArticleList(null)
        );

        assertEquals(ErrorCode.PARAM_ERROR.getCode(), exception.getCode());
        verifyNoInteractions(articleMapper, commentCountService);
    }

    @Test
    @DisplayName("文章ID集合为空时不查询文章简要信息")
    void getArticleBriefByIds_emptyIds_returnsEmptyMap() {
        assertTrue(articleService.getArticleBriefByIds(Set.of()).isEmpty());

        verify(articleMapper, never()).getArticleBriefByIds(Set.of());
    }

    @Test
    @DisplayName("文章ID列表为空时不查询文章列表项")
    void getArticleListByIds_emptyIds_returnsEmptyList() {
        assertTrue(articleService.getArticleListByIds(List.of()).isEmpty());

        verify(articleMapper, never()).getArticleListByIds(List.of());
        verifyNoInteractions(commentCountService);
    }
}
