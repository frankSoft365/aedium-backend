package com.microsoft.aediumbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.microsoft.aediumbackend.commen.CursorPage;
import com.microsoft.aediumbackend.model.dto.article.ArticlePublishRequest;
import com.microsoft.aediumbackend.model.dto.article.request.HomeArticleListRequest;
import com.microsoft.aediumbackend.model.dto.article.request.UserArticleListRequest;
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
     * 获取公开主页文章列表
     */
    CursorPage<ArticleListItemVO> getPublicHomeArticleList(HomeArticleListRequest req);


    /**
     * 获取文章列表
     */
    List<ArticleListItemVO> getUserArticleList(Long userId);

    /**
     * 内部调用
     * 根据文章id集合获取文章预览列表
     */
    Map<Long, ArticleListItemVO> getArticleListItemVOByIds(List<Long> articleIds, boolean hasResponseCount);

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

    /**
     * 根据ID列表批量获取文章列表项（含作者信息、评论数聚合；仅返回正常文章，已删除文章不包含）
     */
    List<ArticleListItemVO> getArticleListByIds(List<Long> articleIds);

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
}
