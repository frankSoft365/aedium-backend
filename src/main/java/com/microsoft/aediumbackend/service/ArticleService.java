package com.microsoft.aediumbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.microsoft.aediumbackend.model.dto.article.ArticlePublishRequest;
import com.microsoft.aediumbackend.model.dto.article.request.ArticleListRequest;
import com.microsoft.aediumbackend.model.dto.article.response.ArticleBriefDTO;
import com.microsoft.aediumbackend.model.entity.Article;
import com.microsoft.aediumbackend.model.vo.ArticleListItemVO;
import com.microsoft.aediumbackend.model.vo.ArticleVO;
import com.microsoft.aediumbackend.model.vo.TopicInArticleVO;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ArticleService extends IService<Article> {

    /**
     * 获取文章列表
     */
    List<ArticleListItemVO> getArticleList(ArticleListRequest req);

    /**
     * 发布
     */
    Long publish(ArticlePublishRequest publishRequest);

    /**
     * 获得一个article
     */
    ArticleVO getArticleById(Long id);

    /**
     * 获得这个article的topics
     */
    List<TopicInArticleVO> getTopicsOfArticleById(Long articleId);

    /**
     * 删除文章
     */
    void deleteArticle(Long articleId);

    /**
     * 根据ID列表获取文章简要信息
     */
    Map<Long, ArticleBriefDTO> getArticleBriefByIds(Set<Long> articleIds);
}
