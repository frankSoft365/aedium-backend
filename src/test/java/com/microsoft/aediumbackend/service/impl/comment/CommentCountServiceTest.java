package com.microsoft.aediumbackend.service.impl.comment;

import com.microsoft.aediumbackend.mapper.CommentMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentCountServiceTest {

    @Mock
    private CommentMapper commentMapper;

    @InjectMocks
    private CommentCountService commentCountService;

    @Test
    @DisplayName("文章ID列表为null时不查询数据库")
    void getCommentCountForArticles_nullIds_returnsEmptyMap() {
        assertTrue(commentCountService.getCommentCountForArticles(null).isEmpty());

        verify(commentMapper, never()).findCommentCountForArticleIds(null);
    }

    @Test
    @DisplayName("文章ID列表为空时不查询数据库")
    void getCommentCountForArticles_emptyIds_returnsEmptyMap() {
        assertTrue(commentCountService.getCommentCountForArticles(List.of()).isEmpty());

        verify(commentMapper, never()).findCommentCountForArticleIds(List.of());
    }

    @Test
    @DisplayName("评论数查询结果为空时返回空Map")
    void getCommentCountForArticles_emptyResult_returnsEmptyMap() {
        List<Long> articleIds = List.of(1L);
        org.mockito.Mockito.when(commentMapper.findCommentCountForArticleIds(articleIds))
                .thenReturn(List.of());

        assertTrue(commentCountService.getCommentCountForArticles(articleIds).isEmpty());
    }
}
