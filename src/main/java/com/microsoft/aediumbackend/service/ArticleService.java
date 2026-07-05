package com.microsoft.aediumbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.microsoft.aediumbackend.model.dto.article.ArticlePublishRequest;
import com.microsoft.aediumbackend.model.entity.Article;
import com.microsoft.aediumbackend.model.vo.ArticleVO;

public interface ArticleService extends IService<Article> {

    /**
     * 发布
     */
    Long publish(ArticlePublishRequest publishRequest);

    /**
     * 获得一个article
     */
    ArticleVO getArticleById(Long id);

    /**
     * 删除文章
     */
    void deleteArticle(Long articleId);
}
