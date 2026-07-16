package com.microsoft.aediumbackend.controller;

import com.microsoft.aediumbackend.commen.DeleteRequest;
import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.commen.Result;
import com.microsoft.aediumbackend.exception.BusinessException;
import com.microsoft.aediumbackend.mapper.ArticleMapper;
import com.microsoft.aediumbackend.model.dto.article.ArticlePublishRequest;
import com.microsoft.aediumbackend.model.dto.article.ArticleUpdateRequest;
import com.microsoft.aediumbackend.model.entity.Article;
import com.microsoft.aediumbackend.model.vo.ArticleListItemVO;
import com.microsoft.aediumbackend.model.vo.ArticleVO;
import com.microsoft.aediumbackend.service.ArticleService;
import com.microsoft.aediumbackend.utils.CurrentHold;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.*;

@RestController
@RequestMapping("/article")
public class ArticleController {

    @Resource
    private ArticleService articleService;
    @Resource
    private ArticleMapper articleMapper;

    /**
     * 发布文章
     */
    @PostMapping("/user/publish")
    public Result<Long> publishArticle(@RequestBody ArticlePublishRequest publishRequest) {
        if (publishRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_EMPTY);
        }
        Long articleId = articleService.publish(publishRequest);
        return Result.success(articleId);
    }

    /**
     * 获取文章列表
     */
    @PostMapping("/public/list")
    public Result<List<ArticleListItemVO>> getArticleList(@RequestBody Object param) {
        List<ArticleListItemVO> articleList = articleMapper.getArticleList();
        return Result.success(articleList);
    }

    /**
     * 根据id获取一个文章
     */
    @GetMapping("/public/{id}")
    public Result<ArticleVO> getArticleById(@PathVariable Long id) {
        ArticleVO articleVO = articleService.getArticleById(id);
        return Result.success(articleVO);
    }

    /**
     * 删除文章
     */
    @PostMapping("/user/delete")
    public Result<Void> deleteArticle(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_EMPTY);
        }
        
        Long userId = CurrentHold.getCurrentId();
        Long articleId = deleteRequest.getId();
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, ARTICLE_NOT_FOUND);
        }
        if (!article.getAuthorId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, NO_AUTH_EDIT_ARTICLE);
        }
        
        articleService.deleteArticle(articleId);
        return Result.success();
    }

    /**
     * 更新
     */
    @PostMapping("/user/update")
    public Result<Void> updateArticleById(@RequestBody ArticleUpdateRequest updateRequest) {
        if (updateRequest == null || updateRequest.getArticleId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_EMPTY);
        }
        Long currentUserId = CurrentHold.getCurrentId();
        Article article = articleMapper.selectById(updateRequest.getArticleId());
        if (article == null || article.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, ARTICLE_NOT_FOUND);
        }
        if (!article.getAuthorId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, NO_AUTH_EDIT_ARTICLE);
        }
        // only article
        int rows = articleMapper.updateArticleInfo(
                updateRequest.getArticleId(),
                updateRequest.getContent(),
                updateRequest.getTitle(),
                updateRequest.getSubtitle(),
                updateRequest.getCoverImage(),
                updateRequest.getCoverFocusY()
        );
        if (rows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, ARTICLE_NOT_FOUND);
        }
        
        return Result.success();
    }
}
