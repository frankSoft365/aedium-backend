package com.microsoft.aediumbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.exception.BusinessException;
import com.microsoft.aediumbackend.mapper.ArticleMapper;
import com.microsoft.aediumbackend.mapper.CollectionListArticleMapper;
import com.microsoft.aediumbackend.mapper.CollectionListMapper;
import com.microsoft.aediumbackend.model.dto.collection.CollectionListCreateRequest;
import com.microsoft.aediumbackend.model.dto.collection.CollectionListUpdateRequest;
import com.microsoft.aediumbackend.model.dto.collection.ListCoverDTO;
import com.microsoft.aediumbackend.model.entity.Article;
import com.microsoft.aediumbackend.model.entity.CollectionList;
import com.microsoft.aediumbackend.model.entity.CollectionListArticle;
import com.microsoft.aediumbackend.model.vo.ArticleCollectStatusVO;
import com.microsoft.aediumbackend.model.vo.ArticleListItemVO;
import com.microsoft.aediumbackend.model.vo.CollectionListInfoVO;
import com.microsoft.aediumbackend.model.vo.CollectionListVO;
import com.microsoft.aediumbackend.model.vo.ListCoverItemVO;
import com.microsoft.aediumbackend.utils.CurrentHold;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.*;

@Service
@Slf4j
public class CollectionListService {

    private static final String DEFAULT_LIST_NAME = "Reading list";
    private static final int COVER_PREVIEW_LIMIT = 3;
    private static final int PUBLIC = 1;
    private static final int PRIVATE = 0;

    @Resource
    private CollectionListMapper collectionListMapper;
    @Resource
    private CollectionListArticleMapper collectionListArticleMapper;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private ArticleService articleService;

    // ==================== 查询需求 ====================

    /**
     * 查询需求1：查询用户的所有列表（含封面预览）。
     * 非本人则只查公开
     */
    @Transactional(rollbackFor = Exception.class)
    public List<CollectionListVO> getUserLists(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_INVALID);
        }
        Long currentUserId = currentUserId();
        List<CollectionList> entities = listByUser(userId, currentUserId);
        if (entities.isEmpty()) {
            return List.of();
        }
        List<CollectionListVO> lists = entities.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        fillCovers(lists);
        return lists;
    }

    /**
     * 查询需求2：查询某个列表中的所有文章（复用 ArticleService 聚合）。
     * 仅允许列表拥有者或公开列表的访问。
     */
    public List<ArticleListItemVO> getListArticles(Long listId) {
        Long userId = currentUserId();
        getAccessibleList(listId, userId);
        // 1. MP 查询列表中的文章ID（按收藏时间倒序）
        QueryWrapper<CollectionListArticle> qw = new QueryWrapper<>();
        qw.eq("list_id", listId).orderByDesc("create_time");
        List<Long> articleIds = collectionListArticleMapper.selectList(qw).stream()
                .map(CollectionListArticle::getArticleId)
                .collect(Collectors.toList());
        if (articleIds.isEmpty()) {
            return new ArrayList<>();
        }
        // 2. 复用 ArticleService 聚合（作者信息 + 评论数）
        List<ArticleListItemVO> articleList = articleService.getArticleListByIds(articleIds);
        Map<Long, ArticleListItemVO> articleMap = articleList.stream()
                .collect(Collectors.toMap(ArticleListItemVO::getId, v -> v));
        // 3. 按收藏时间倒序组装，已删除文章构造 id-only VO
        List<ArticleListItemVO> result = new ArrayList<>(articleIds.size());
        for (Long articleId : articleIds) {
            ArticleListItemVO vo = articleMap.get(articleId);
            if (vo != null) {
                result.add(vo);
            } else {
                ArticleListItemVO deleted = new ArticleListItemVO();
                deleted.setId(articleId);
                result.add(deleted);
            }
        }
        return result;
    }

    /**
     * 根据 id 查询某个列表的基础信息（不含封面预览）。
     */
    public CollectionListInfoVO getListInfo(Long listId) {
        Long userId = currentUserId();
        CollectionList list = getAccessibleList(listId, userId);
        return new CollectionListInfoVO(
                list.getId(), list.getUserId(), list.getName(), list.getDescription(),
                list.getIsPublic(), list.getIsDefault(), list.getArticleCount()
        );
    }

    /**
     * 收藏文章到指定列表。
     * listId 为空 → 加到默认列表；不为空 → 加到指定列表（需校验归属）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void addArticleToList(Long articleId, Long listId) {
        Long userId = currentUserId();
        if (articleId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_EMPTY);
        }
        Article article = articleMapper.selectById(articleId);
        if (article == null || article.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, COLLECTION_ARTICLE_NOT_FOUND);
        }

        Long targetListId;
        if (listId == null) {
            // 场景 1：未传 listId，加到默认列表（若无则创建）
            targetListId = getDefaultList(userId).getId();
        } else {
            // 场景 2：传了 listId，校验归属后加到指定列表
            getOwnedList(listId, userId);
            targetListId = listId;
        }
        int rows = collectionListArticleMapper.insertIgnore(targetListId, articleId);
        if (rows > 0) {
            incrementArticleCount(targetListId);
        }
    }

    /**
     * 查询需求6：创建自定义列表。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createCustomList(CollectionListCreateRequest req) {
        Long userId = currentUserId();
        CollectionList list = new CollectionList();
        list.setUserId(userId);
        list.setName(req.getName());
        list.setDescription(req.getDescription());
        list.setIsPublic(req.getIsPublic() == null ? PRIVATE : req.getIsPublic());
        list.setIsDefault(0);
        list.setArticleCount(0);
        list.setIsDelete(0);
        collectionListMapper.insert(list);
        return list.getId();
    }

    /**
     * 查询需求7：删除某条 list-article 关系（取消收藏）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeArticleFromList(Long listId, Long articleId) {
        Long userId = currentUserId();
        getOwnedList(listId, userId);
        int rows = collectionListArticleMapper.deleteRelation(listId, articleId);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, COLLECTION_ARTICLE_NOT_IN_LIST);
        }
        decrementArticleCount(listId);
    }

    /**
     * 批量取消收藏（从指定列表中移除多篇文章）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeArticlesFromList(Long listId, List<Long> articleIds) {
        Long userId = currentUserId();
        getOwnedList(listId, userId);
        QueryWrapper<CollectionListArticle> qw = new QueryWrapper<>();
        qw.eq("list_id", listId).in("article_id", articleIds);
        int rows = collectionListArticleMapper.delete(qw);
        if (rows > 0) {
            UpdateWrapper<CollectionList> uw = new UpdateWrapper<>();
            uw.eq("id", listId).setSql("article_count = GREATEST(article_count - " + rows + ", 0)");
            collectionListMapper.update(null, uw);
        }
    }

    /**
     * 删除自定义收藏列表。逻辑删除 list，物理删除关联关系。
     * 默认列表不允许删除。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteList(Long listId) {
        Long userId = currentUserId();
        CollectionList list = getOwnedList(listId, userId);
        if (list.getIsDefault() == 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, COLLECTION_DEFAULT_LIST_NOT_DELETABLE);
        }
        // 逻辑删除 list
        UpdateWrapper<CollectionList> uw = new UpdateWrapper<>();
        uw.eq("id", listId).set("is_delete", 1);
        collectionListMapper.update(null, uw);
        // 物理删除关联关系
        QueryWrapper<CollectionListArticle> rqw = new QueryWrapper<>();
        rqw.eq("list_id", listId);
        collectionListArticleMapper.delete(rqw);
    }

    /**
     * 切换列表公开/私有状态（0→1, 1→0）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggleListPublic(Long listId) {
        Long userId = currentUserId();
        CollectionList list = getOwnedList(listId, userId);
        int newStatus = list.getIsPublic() == PUBLIC ? PRIVATE : PUBLIC;
        UpdateWrapper<CollectionList> uw = new UpdateWrapper<>();
        uw.eq("id", listId).eq("is_delete", 0).set("is_public", newStatus);
        collectionListMapper.update(null, uw);
    }

    /**
     * 更新自定义收藏列表信息（部分字段更新：null 字段不更新）。
     * 仅允许更新自己的列表，且不允许修改默认列表的 name/description。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateListInfo(CollectionListUpdateRequest req) {
        Long userId = currentUserId();
        CollectionList list = getOwnedList(req.getListId(), userId);

        boolean hasAny = false;
        UpdateWrapper<CollectionList> uw = new UpdateWrapper<>();
        uw.eq("id", req.getListId()).eq("is_delete", 0);

        if (req.getName() != null) {
            if (list.getIsDefault() == 1) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, COLLECTION_DEFAULT_LIST_NOT_EDITABLE);
            }
            if (req.getName().isBlank()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, COLLECTION_LIST_NAME_EMPTY);
            }
            uw.set("name", req.getName());
            hasAny = true;
        }

        if (req.getDescription() != null) {
            if (list.getIsDefault() == 1) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, COLLECTION_DEFAULT_LIST_NOT_EDITABLE);
            }
            uw.set("description", req.getDescription());
            hasAny = true;
        }

        if (req.getIsPublic() != null) {
            if (req.getIsPublic() != PRIVATE && req.getIsPublic() != PUBLIC) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, COLLECTION_PUBLIC_INVALID);
            }
            uw.set("is_public", req.getIsPublic());
            hasAny = true;
        }

        if (hasAny) {
            collectionListMapper.update(null, uw);
        }
    }

    // ==================== 工作流支持 ====================

    /**
     * 查询某篇文章在当前用户所有列表中的收藏情况。
     */
    public List<ArticleCollectStatusVO> getArticleCollectStatus(Long articleId) {
        Long userId = currentUserId();
        if (articleId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_EMPTY);
        }
        List<CollectionList> lists = listByUser(userId);
        if (lists.isEmpty()) {
            return List.of();
        }
        List<Long> listIds = lists.stream().map(CollectionList::getId).collect(Collectors.toList());
        // MP 查询文章被收藏到哪些列表
        QueryWrapper<CollectionListArticle> relQw = new QueryWrapper<>();
        relQw.eq("article_id", articleId).in("list_id", listIds);
        Set<Long> collectedSet = collectionListArticleMapper.selectList(relQw).stream()
                .map(CollectionListArticle::getListId).collect(Collectors.toSet());
        return lists.stream()
                .map(l -> new ArticleCollectStatusVO(l.getId(), l.getName(), l.getIsDefault(), l.getIsPublic(), collectedSet.contains(l.getId())))
                .collect(Collectors.toList());
    }

    // ==================== 内部方法 ====================

    /**
     * 查询当前用户是否收藏了某篇文章（任意列表中收藏即视为已收藏）。
     */
    public boolean isArticleCollected(Long articleId) {
        Long userId = currentUserId();
        if (articleId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_EMPTY);
        }
        List<Long> listIds = listByUser(userId).stream()
                .map(CollectionList::getId).collect(Collectors.toList());
        if (listIds.isEmpty()) {
            return false;
        }
        QueryWrapper<CollectionListArticle> qw = new QueryWrapper<>();
        qw.eq("article_id", articleId).in("list_id", listIds);
        return collectionListArticleMapper.selectCount(qw) > 0;
    }

    /**
     * MP：查询用户的所有列表（默认在前，按创建时间升序）
     */
    private List<CollectionList> listByUser(Long userId) {
        QueryWrapper<CollectionList> qw = new QueryWrapper<>();
        qw.eq("user_id", userId).orderByDesc("is_default").orderByAsc("create_time");
        return collectionListMapper.selectList(qw);
    }

    /**
     * 查询用户的所有收藏夹
     * 需要关注权限
     *
     * @param userId        某个用户的收藏夹的id
     * @param currentUserId 当前用户
     * @return 未聚合的收藏夹列表
     */
    private List<CollectionList> listByUser(Long userId, Long currentUserId) {
        QueryWrapper<CollectionList> qw = new QueryWrapper<>();
        qw.eq("user_id", userId).orderByDesc("is_default").orderByAsc("create_time");
        if (!currentUserId.equals(userId)) {
            qw.eq("is_public", PUBLIC);
        }
        return collectionListMapper.selectList(qw);
    }

    /**
     * MP：查询用户的默认列表
     */
    private CollectionList getDefaultList(Long userId) {
        QueryWrapper<CollectionList> qw = new QueryWrapper<>();
        qw.eq("user_id", userId).eq("is_default", 1).last("LIMIT 1");
        CollectionList defaultList = collectionListMapper.selectOne(qw);
        if (defaultList == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, DEFAULT_COLLECTION_LIST_NOT_FOUND);
        }
        return defaultList;
    }

    /**
     * MP：列表文章数 +1
     */
    private void incrementArticleCount(Long listId) {
        UpdateWrapper<CollectionList> uw = new UpdateWrapper<>();
        uw.eq("id", listId).setSql("article_count = article_count + 1");
        collectionListMapper.update(null, uw);
    }

    /**
     * MP：列表文章数 -1（不低于 0）
     */
    private void decrementArticleCount(Long listId) {
        UpdateWrapper<CollectionList> uw = new UpdateWrapper<>();
        uw.eq("id", listId).setSql("article_count = GREATEST(article_count - 1, 0)");
        collectionListMapper.update(null, uw);
    }

    /**
     * 为用户创建默认收藏夹
     * 仅注册时创建一次
     *
     * @param userId 用户的id
     * @return 创建的默认收藏夹的id
     */
    public CollectionList createDefaultList(Long userId) {
        CollectionList list = new CollectionList();
        list.setUserId(userId);
        list.setName(DEFAULT_LIST_NAME);
        list.setDescription(null);
        list.setIsPublic(PUBLIC);
        list.setIsDefault(1);
        list.setArticleCount(0);
        list.setIsDelete(0);
        collectionListMapper.insert(list);
        return list;
    }

    /**
     * 校验列表归属当前用户（写操作）
     */
    private CollectionList getOwnedList(Long listId, Long userId) {
        CollectionList list = collectionListMapper.selectById(listId);
        if (list == null || list.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, COLLECTION_LIST_NOT_FOUND);
        }
        if (!list.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, COLLECTION_LIST_NOT_OWNED);
        }
        return list;
    }

    /**
     * 校验列表可被当前用户访问（读操作）
     */
    private CollectionList getAccessibleList(Long listId, Long userId) {
        CollectionList list = collectionListMapper.selectById(listId);
        if (list == null || list.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, COLLECTION_LIST_NOT_FOUND);
        }
        boolean isOwner = list.getUserId().equals(userId);
        if (!isOwner && list.getIsPublic() != PUBLIC) {
            throw new BusinessException(ErrorCode.NO_AUTH, COLLECTION_LIST_NOT_OWNED);
        }
        return list;
    }

    /**
     * 实体 → 列表 VO（不含封面，封面由 fillCovers 填充）
     */
    private CollectionListVO toVO(CollectionList entity) {
        return new CollectionListVO(
                entity.getId(), entity.getUserId(), entity.getName(), entity.getDescription(),
                entity.getIsPublic(), entity.getIsDefault(), entity.getArticleCount(),
                new ArrayList<>()
        );
    }

    /**
     * 填充每个列表的封面预览（唯一保留的自定义 SQL —— 窗口函数分组 Top-N）
     */
    private void fillCovers(List<CollectionListVO> lists) {
        if (lists.isEmpty()) {
            return;
        }
        List<Long> listIds = lists.stream().map(CollectionListVO::getId).collect(Collectors.toList());
        List<ListCoverDTO> covers = collectionListMapper.findListCovers(listIds, COVER_PREVIEW_LIMIT);
        Map<Long, List<ListCoverItemVO>> coverMap = covers.stream()
                .collect(Collectors.groupingBy(
                        ListCoverDTO::getListId,
                        Collectors.mapping(c -> new ListCoverItemVO(c.getArticleId(), c.getCoverImage(), c.getIsDelete()), Collectors.toList())));
        for (CollectionListVO l : lists) {
            l.setCoverImages(coverMap.getOrDefault(l.getId(), new ArrayList<>()));
        }
    }

    private Long currentUserId() {
        Long userId = CurrentHold.getCurrentId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.NO_AUTH, PARAM_INVALID);
        }
        return userId;
    }
}
