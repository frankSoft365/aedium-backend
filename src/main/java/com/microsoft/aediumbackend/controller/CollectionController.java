package com.microsoft.aediumbackend.controller;

import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.commen.Result;
import com.microsoft.aediumbackend.exception.BusinessException;
import com.microsoft.aediumbackend.model.dto.collection.CollectionArticleAddRequest;
import com.microsoft.aediumbackend.model.dto.collection.CollectionArticleBatchRemoveRequest;
import com.microsoft.aediumbackend.model.dto.collection.CollectionArticleRemoveRequest;
import com.microsoft.aediumbackend.model.dto.collection.CollectionListCreateRequest;
import com.microsoft.aediumbackend.model.dto.collection.CollectionListUpdatePublicRequest;
import com.microsoft.aediumbackend.model.dto.collection.CollectionListUpdateRequest;
import com.microsoft.aediumbackend.model.vo.ArticleCollectStatusVO;
import com.microsoft.aediumbackend.model.vo.ArticleListItemVO;
import com.microsoft.aediumbackend.model.vo.CollectionListInfoVO;
import com.microsoft.aediumbackend.model.vo.CollectionListVO;
import com.microsoft.aediumbackend.service.CollectionListService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.PARAM_EMPTY;

@RestController
@RequestMapping("/collection")
public class CollectionController {

    @Resource
    private CollectionListService collectionListService;

    /**
     * 查询用户的所有收藏列表（个人主页 Lists 页）。
     * 若用户无任何列表，自动创建并返回空的默认列表。
     */
    @GetMapping("/user/lists")
    public Result<List<CollectionListVO>> getUserLists() {
        return Result.success(collectionListService.getUserLists());
    }

    /**
     * 创建自定义收藏列表（收集 name、description、是否公开）。
     */
    @PostMapping("/user/list/create")
    public Result<Long> createList(@Valid @RequestBody CollectionListCreateRequest req) {
        return Result.success(collectionListService.createCustomList(req));
    }

    /**
     * 切换收藏列表公开/私有状态（取反）。
     */
    @PostMapping("/user/list/public")
    public Result<Void> toggleListPublic(@Valid @RequestBody CollectionListUpdatePublicRequest req) {
        collectionListService.toggleListPublic(req.getListId());
        return Result.success();
    }

    /**
     * 更新收藏列表信息（部分字段更新，null 字段不更新）。
     * 仅允许更新自己的自定义列表；默认列表不允许修改名称和描述。
     */
    @PostMapping("/user/list/update")
    public Result<Void> updateListInfo(@Valid @RequestBody CollectionListUpdateRequest req) {
        collectionListService.updateListInfo(req);
        return Result.success();
    }

    /**
     * 收藏文章。listId 为空 → 加到默认列表（若无则创建）；不为空 → 加到指定列表。
     */
    @PostMapping("/user/article/add")
    public Result<Void> addArticle(@RequestBody CollectionArticleAddRequest req) {
        if (req == null || req.getArticleId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_EMPTY);
        }
        collectionListService.addArticleToList(req.getArticleId(), req.getListId());
        return Result.success();
    }

    /**
     * 取消收藏（点击列表中某条 checkbox）。
     * 成功后由前端重新拉取该文章在用户所有列表的收藏情况。
     */
    @PostMapping("/user/article/remove")
    public Result<Void> removeArticle(@RequestBody CollectionArticleRemoveRequest req) {
        if (req == null || req.getListId() == null || req.getArticleId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_EMPTY);
        }
        collectionListService.removeArticleFromList(req.getListId(), req.getArticleId());
        return Result.success();
    }

    /**
     * 批量取消收藏（列表详情页移除多篇文章）。
     */
    @PostMapping("/user/article/remove/batch")
    public Result<Void> removeArticles(@Valid @RequestBody CollectionArticleBatchRemoveRequest req) {
        if (req.getListId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_EMPTY);
        }
        collectionListService.removeArticlesFromList(req.getListId(), req.getArticleIds());
        return Result.success();
    }

    /**
     * 查询某篇文章在当前用户所有列表中的收藏情况（点击已收藏文章的收藏按钮）。
     */
    @GetMapping("/user/article/status")
    public Result<List<ArticleCollectStatusVO>> getArticleStatus(@RequestParam Long articleId) {
        if (articleId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_EMPTY);
        }
        return Result.success(collectionListService.getArticleCollectStatus(articleId));
    }

    /**
     * 查询当前用户是否收藏了某篇文章（任意列表中收藏即视为已收藏）。
     */
    @GetMapping("/user/article/collected")
    public Result<Boolean> isArticleCollected(@RequestParam Long articleId) {
        if (articleId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_EMPTY);
        }
        return Result.success(collectionListService.isArticleCollected(articleId));
    }

    /**
     * 查询某个列表中的所有文章（列表详情页，不含正文，仅链接所需信息）。
     */
    @GetMapping("/user/list/{listId}/articles")
    public Result<List<ArticleListItemVO>> getListArticles(@PathVariable Long listId) {
        return Result.success(collectionListService.getListArticles(listId));
    }

    /**
     * 根据 id 查询某个收藏列表的基础信息（不含封面预览）。
     */
    @GetMapping("/user/list/{listId}")
    public Result<CollectionListInfoVO> getListInfo(@PathVariable Long listId) {
        return Result.success(collectionListService.getListInfo(listId));
    }

    /**
     * 删除自定义收藏列表（默认列表不可删）。
     */
    @PostMapping("/user/list/{listId}/delete")
    public Result<Void> deleteList(@PathVariable Long listId) {
        collectionListService.deleteList(listId);
        return Result.success();
    }
}
