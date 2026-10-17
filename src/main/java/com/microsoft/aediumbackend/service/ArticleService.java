package com.microsoft.aediumbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.microsoft.aediumbackend.commen.CursorPage;
import com.microsoft.aediumbackend.model.dto.article.request.ArticlePublishRequest;
import com.microsoft.aediumbackend.model.dto.article.request.HomeArticleListRequest;
import com.microsoft.aediumbackend.model.dto.article.request.Operation;
import com.microsoft.aediumbackend.model.dto.article.request.UserArticleListRequest;
import com.microsoft.aediumbackend.model.dto.article.response.*;
import com.microsoft.aediumbackend.model.entity.Article;
import com.microsoft.aediumbackend.model.vo.TopicInArticleVO;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ArticleService extends IService<Article> {

    /**
     * 获取公开主页文章列表
     */
    CursorPage<ArticlePreview> getPublicHomeArticleList(HomeArticleListRequest req);


    /**
     * 获取用户作为作者的文章列表 包含pinned
     */
    CursorPage<ArticlePreview> getUserArticleList(UserArticleListRequest req);

    /**
     * 内部调用
     * 根据文章id集合获取文章预览列表
     */
    Map<Long, ArticlePreview> getArticlePreviewByIds(List<Long> articleIds, boolean hasResponseCount);

    /**
     * 发布
     */
    Long publish(ArticlePublishRequest publishRequest);

    /**
     * 获得一个article
     */
    ArticleDetail getArticleDetailById(Long id);

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

    /**
     * 校验文章id的有效性
     * 即校验文章是否存在
     * 已被删除的文章的id被视为无效
     */
    void validateArticleId(Long articleId);

    /**
     * 校验文章id的有效性同时校验是否作者是本人
     * 是本人则报错：对自己的文章进行某种操作
     * 即校验文章是否存在
     * 已被删除的文章的id被视为无效
     */
    void validateArticleId(Long articleId, Long userId);

    List<OperationResult> getBatchOperationResult(List<Operation> operationList, Long userId);

    /**
     * 作者pin文章
     */
    void pinArticleByCreator(Long articleId, Long authorId);

    /**
     * 作者unpin文章
     */
    void unpinArticleByCreator(Long articleId, Long authorId);
}
