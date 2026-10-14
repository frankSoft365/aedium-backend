package com.microsoft.aediumbackend.controller;

import com.microsoft.aediumbackend.commen.CursorPage;
import com.microsoft.aediumbackend.commen.DeleteRequest;
import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.commen.Result;
import com.microsoft.aediumbackend.exception.BusinessException;
import com.microsoft.aediumbackend.mapper.ArticleMapper;
import com.microsoft.aediumbackend.model.dto.article.request.ArticlePublishRequest;
import com.microsoft.aediumbackend.model.dto.article.request.ArticleUpdateRequest;
import com.microsoft.aediumbackend.model.dto.article.request.HomeArticleListRequest;
import com.microsoft.aediumbackend.model.dto.article.request.OperationList;
import com.microsoft.aediumbackend.model.dto.article.request.UserArticleListRequest;
import com.microsoft.aediumbackend.model.dto.article.response.ArticleDetail;
import com.microsoft.aediumbackend.model.dto.article.response.ArticlePreview;
import com.microsoft.aediumbackend.model.dto.article.response.ArticleResult;
import com.microsoft.aediumbackend.model.entity.Article;
import com.microsoft.aediumbackend.service.ArticleService;
import com.microsoft.aediumbackend.utils.CurrentHold;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
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
    public Result<Long> publishArticle(@Valid @RequestBody ArticlePublishRequest publishRequest) {
        if (publishRequest == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_EMPTY);
        }
        Long articleId = articleService.publish(publishRequest);
        return Result.success(articleId);
    }

    /**
     * 获取公开的主页文章列表 游标查询
     */
    @PostMapping("/public/home/list")
    public Result<CursorPage<ArticlePreview>> getPublicHomeArticleList(
            @Valid @RequestBody HomeArticleListRequest req
    ) {
        return Result.success(articleService.getPublicHomeArticleList(req));
    }

    /**
     * 获取用户自己的文章列表 游标查询
     */
    @PostMapping("/user/list")
    public Result<CursorPage<ArticlePreview>> getUserArticleList(
            @Valid @RequestBody UserArticleListRequest req
    ) {
        return Result.success(articleService.getUserArticleList(req));
    }

    /**
     * 根据id获取公开的文章详情
     */
    @GetMapping("/public/{id}")
    public Result<ArticleDetail> getArticleDetailById(@PathVariable Long id) {
        return Result.success(articleService.getArticleDetailById(id));
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
     * 编辑
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

    @PostMapping("/articleResult")
    public Result<List<ArticleResult>> batchQueryCountOrViewerEdge(
            @Valid @RequestBody OperationList list
    ) {
        if (list == null || list.getOperationList().isEmpty()) {
            return Result.success(List.of());
        }
        Long userId = CurrentHold.getCurrentId();
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_INVALID);
        }
        return Result.success(articleService.getArticleResultList(list.getOperationList(), userId));
    }
}
