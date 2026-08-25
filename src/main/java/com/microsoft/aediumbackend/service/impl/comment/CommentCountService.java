package com.microsoft.aediumbackend.service.impl.comment;

import com.microsoft.aediumbackend.mapper.CommentMapper;
import com.microsoft.aediumbackend.model.dto.comment.response.ArticleCommentCount;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommentCountService {

    private final CommentMapper commentMapper;

    public CommentCountService(CommentMapper commentMapper) {
        this.commentMapper = commentMapper;
    }

    /**
     * 获取每个文章的评论数
     */
    public Map<Long, Integer> getCommentCountForArticles(List<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ArticleCommentCount> commentCountForArticleIds = commentMapper.findCommentCountForArticleIds(articleIds);
        if (commentCountForArticleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return commentCountForArticleIds.stream()
                .collect(Collectors.toMap(ArticleCommentCount::getArticleId, ArticleCommentCount::getCommentCount));
    }
}
