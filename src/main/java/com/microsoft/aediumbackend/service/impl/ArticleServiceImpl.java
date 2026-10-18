package com.microsoft.aediumbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.microsoft.aediumbackend.commen.CursorPage;
import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.exception.BusinessException;
import com.microsoft.aediumbackend.mapper.ArticleMapper;
import com.microsoft.aediumbackend.mapper.ArticleTopicMapper;
import com.microsoft.aediumbackend.mapper.TopicMapper;
import com.microsoft.aediumbackend.model.dto.article.request.ArticlePublishRequest;
import com.microsoft.aediumbackend.model.dto.article.request.HomeArticleListRequest;
import com.microsoft.aediumbackend.model.dto.article.request.Operation;
import com.microsoft.aediumbackend.model.dto.article.request.UserArticleListRequest;
import com.microsoft.aediumbackend.model.dto.article.response.*;
import com.microsoft.aediumbackend.model.dto.collection.response.CollectionsConnection;
import com.microsoft.aediumbackend.model.entity.Article;
import com.microsoft.aediumbackend.model.entity.ArticleTopic;
import com.microsoft.aediumbackend.model.entity.Topic;
import com.microsoft.aediumbackend.model.enums.OperationNameEnum;
import com.microsoft.aediumbackend.model.enums.PublishStatusEnum;
import com.microsoft.aediumbackend.model.vo.TopicInArticleVO;
import com.microsoft.aediumbackend.service.*;
import com.microsoft.aediumbackend.service.impl.comment.CommentCountService;
import com.microsoft.aediumbackend.service.repost.RepostService;
import com.microsoft.aediumbackend.utils.CurrentHold;
import com.microsoft.aediumbackend.utils.CursorPageUtils;
import com.microsoft.aediumbackend.utils.SlugUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.*;

@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    @Resource
    private TopicService topicService;
    @Resource
    private ArticleTopicService articleTopicService;
    @Resource
    private TopicMapper topicMapper;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private ArticleTopicMapper articleTopicMapper;
    @Resource
    private CommentCountService commentCountService;
    @Lazy
    @Autowired
    private RepostService repostService;
    @Lazy
    @Autowired
    private CollectionListService collectionListService;
    @Lazy
    @Autowired
    private UserFollowService userFollowService;

    @Override
    public CursorPage<ArticlePreview> getPublicHomeArticleList(HomeArticleListRequest req) {
        List<ArticleWithAuthor> cursorList = articleMapper.findAllArticlesCursor(
                req.getLastCreatedAt(),
                req.getLastId(),
                req.getSize() + 1
        );
        if (cursorList.isEmpty()) {
            return new CursorPage<>(Collections.emptyList(), false, null, null);
        }
        CursorPageUtils.CursorInfo cursorInfo = CursorPageUtils.extract(
                cursorList,
                req.getSize(),
                ArticleWithAuthor::getPublishTime,
                ArticleWithAuthor::getId
        );

        List<ArticlePreview> list = cursorList.stream()
                .map(ArticlePreview::toArticlePreview)
                .toList();
        aggregatorCommentCount(list);

        return new CursorPage<>(
                list,
                cursorInfo.isHasMore(),
                cursorInfo.getNextCursorCreatedAt(),
                cursorInfo.getNextCursorId()
        );
    }

    @Override
    public CursorPage<ArticlePreview> getUserArticleList(UserArticleListRequest req) {
        boolean isFirstPage = req.getLastCreatedAt() == null && req.getLastId() == null;
        List<ArticleWithAuthor> pinnedArticles = new ArrayList<>();
        // 判断是否是第一页查询
        // 如果是 查询pinned的内容
        if (isFirstPage) {
            pinnedArticles = articleMapper.selectPinnedArticleWithAuthorByAuthorId(req.getUserId());
        }
        // 接着减去pinned文章数且排除pinned文章查第一页
        // 如果不是 数据库层面排除pinned文章查询
        // 最后拼接
        boolean hasPinned = !pinnedArticles.isEmpty();
        List<ArticleWithAuthor> cursorList = articleMapper.findUserArticlesCursor(
                req.getUserId(),
                req.getLastCreatedAt(),
                req.getLastId(),
                req.getSize() - pinnedArticles.size() + 1
        );
        if (cursorList.isEmpty()) {
            return new CursorPage<>(Collections.emptyList(), false, null, null);
        }
        CursorPageUtils.CursorInfo cursorInfo = CursorPageUtils.extract(
                cursorList,
                req.getSize(),
                ArticleWithAuthor::getPublishTime,
                ArticleWithAuthor::getId
        );
        // 在提取了游标信息后 可以进行合并pinned文章和普通文章
        if (hasPinned) {
            cursorList.addAll(0, pinnedArticles);
        }
        List<ArticlePreview> list = cursorList.stream()
                .map(ArticlePreview::toArticlePreview)
                .toList();

        aggregatorCommentCount(list);

        return new CursorPage<>(
                list,
                cursorInfo.isHasMore(),
                cursorInfo.getNextCursorCreatedAt(),
                cursorInfo.getNextCursorId()
        );
    }

    @Override
    public Map<Long, ArticlePreview> getArticlePreviewByIds(List<Long> articleIds, boolean hasResponseCount) {
        if (articleIds.isEmpty()) {
            return new HashMap<>();
        }
        List<ArticleWithAuthor> articleWithAuthors = articleMapper.selectArticleWithAuthorByIds(articleIds);
        if (articleWithAuthors.isEmpty()) {
            return new HashMap<>();
        }
        List<ArticlePreview> list = articleWithAuthors.stream().map(ArticlePreview::toArticlePreview).toList();
        if (hasResponseCount) {
            aggregatorCommentCount(list);
        }
        return list.stream()
                .collect(Collectors.toMap(
                        ArticlePreview::getId,
                        Function.identity(),
                        (oldVal, newVal) -> newVal
                ));
    }

    private List<ArticlePreview> aggregatorCommentCount(List<ArticlePreview> list) {
        if (list.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> articleIds = list.stream().map(ArticlePreview::getId).toList();
        Map<Long, Integer> articleIdCommentCountMap = commentCountService.getCommentCountForArticles(articleIds);
        list.forEach(item -> item.setResponseCount(articleIdCommentCountMap.getOrDefault(item.getId(), 0)));
        return list;
    }

    private List<ArticleWithAuthor> getUserArticleList() {
        return new ArrayList<>();
    }

    /**
     * 发布
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publish(ArticlePublishRequest publishRequest) {
        String publishStatus = publishRequest.getStatus();
        PublishStatusEnum enumByValue = PublishStatusEnum.getEnumByValue(publishStatus);
        if (enumByValue == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_FORMAT_ERROR);
        }
        if (PublishStatusEnum.PUBLISHED.equals(enumByValue)) {
            Article article = publishRequestToArticle(publishRequest);
            Long userId = CurrentHold.getCurrentId();
            if (userId == null) {
                throw new BusinessException(ErrorCode.NO_AUTH, AUTHOR_NOT_FOUND);
            }
            article.setAuthorId(userId);
            this.save(article);

            Long articleId = article.getId();

            // 保存topic
            List<String> topicNames = publishRequest.getTopics();
            if (topicNames != null && !topicNames.isEmpty()) {
                // topics 最多5个
                if (topicNames.size() > 5) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, ARTICLE_TOPIC_NUM_EXCEEDED);
                }
                handleTopics(topicNames, articleId);
            }

            return articleId;
        }
        if (PublishStatusEnum.SCHEDULED.equals(enumByValue)) {

        }
        return null;
    }

    @Override
    public ArticleDetail getArticleDetailById(Long id) {
        ArticleWithAuthor articleWithAuthor = articleMapper.getArticleWithAuthorById(id);
        if (articleWithAuthor == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, ARTICLE_NOT_FOUND);
        }
        ArticleDetail articleDetail = ArticleDetail.toArticleDetail(articleWithAuthor);
        List<TopicInArticleVO> topics = articleTopicMapper.getTopicsOfArticleById(id);
        // topics 最多5个
        if (topics.size() > 5) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, ARTICLE_TOPIC_NUM_EXCEEDED);
        }
        articleDetail.setTopics(topics);
        List<ArticlePreview> list = new ArrayList<>();
        list.add(articleDetail);
        aggregatorCommentCount(list);
        articleDetail = (ArticleDetail) list.get(0);
        return articleDetail;
    }

    @Override
    public ArticleDetail getPrivateArticleDetailForEditById(Long articleId, Long currentUserId) {
        ArticleWithAuthor articleWithAuthor = articleMapper.getArticleWithAuthorById(articleId);
        if (articleWithAuthor == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, ARTICLE_NOT_FOUND);
        }
        if (!Objects.equals(articleWithAuthor.getAuthorId(), currentUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, NO_AUTH_EDIT_ARTICLE);
        }
        ArticleDetail articleDetail = ArticleDetail.toArticleDetail(articleWithAuthor);
        List<TopicInArticleVO> topics = articleTopicMapper.getTopicsOfArticleById(articleId);
        // topics 最多5个
        if (topics.size() > 5) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, ARTICLE_TOPIC_NUM_EXCEEDED);
        }
        articleDetail.setTopics(topics);
        return articleDetail;
    }

    @Override
    public List<TopicInArticleVO> getTopicsOfArticleById(Long articleId) {
        return articleTopicMapper.getTopicsOfArticleById(articleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArticle(Long articleId) {
        // 1. 查询该文章关联的所有topic
        List<ArticleTopic> articleTopics = articleTopicService.list(
                Wrappers.<ArticleTopic>lambdaQuery().eq(ArticleTopic::getArticleId, articleId)
        );

        if (!articleTopics.isEmpty()) {
            // 2. 减少topic的引用计数
            List<Long> topicIds = articleTopics.stream()
                    .map(ArticleTopic::getTopicId)
                    .collect(Collectors.toList());
            topicMapper.decreaseArticleCountBatch(topicIds);

            // 3. 删除article_topic关系
            articleTopicService.remove(
                    Wrappers.<ArticleTopic>lambdaQuery().eq(ArticleTopic::getArticleId, articleId)
            );
        }

        // 4. 逻辑删除文章
        boolean success = this.removeById(articleId);
        if (!success) {
            throw new BusinessException(ErrorCode.DATABASE_ERROR, DATABASE_DELETE_FAILED);
        }
    }

    private Article publishRequestToArticle(ArticlePublishRequest request) {
        Article article = new Article();
        String title = request.getTitle();
        String subtitle = request.getSubtitle();
        String content = request.getContent();
        String coverImage = request.getCoverImage();
        BigDecimal coverFocusY = request.getCoverFocusY() != null ? request.getCoverFocusY() : new BigDecimal("0.5");

        if (title.length() > 100) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, ARTICLE_TITLE_TOO_LONG);
        }
        if (subtitle != null && subtitle.length() > 140) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, ARTICLE_SUBTITLE_TOO_LONG);
        }

        article.setTitle(title);
        article.setSubtitle(subtitle);
        article.setContent(content);
        article.setCoverImage(coverImage);
        article.setCoverFocusY(coverFocusY);
        return article;
    }

    /**
     * 如果有topic 进行处理
     * topic 要求
     * topic slug 不能超过25字符
     * topics 不能有重复的topic
     */
    private void handleTopics(List<String> topicNames, Long articleId) {
        List<ArticleTopic> articleTopicList = new ArrayList<>();

        // 尝试插入topics
        List<Topic> topicsToProcess = topicNames.stream().map(topicName -> {
            Topic topic = new Topic();
            topic.setName(topicName);

            // 校验topic不能超过25字符
            String slug = SlugUtils.makeSlug(topicName);
            if (slug.length() > 25) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, ARTICLE_TOPIC_TOO_LONG);
            }

            topic.setSlug(slug);
            return topic;
        }).filter(distinctByKey(Topic::getSlug)).toList();
        topicMapper.insertIgnoreBatch(topicsToProcess);

        // 查询topic id
        List<Topic> topicsProcessed = topicService.list(Wrappers.<Topic>lambdaQuery().in(Topic::getSlug, topicsToProcess.stream().map(Topic::getSlug).toList()));
        List<Long> topicsIdsPrecessed = topicsProcessed.stream().map(Topic::getId).toList();

        // 增加topic引用计数
        topicMapper.increaseArticleCountBatch(topicsIdsPrecessed);

        // all topics saved in relation
        topicsIdsPrecessed.forEach(id -> {
            articleTopicList.add(new ArticleTopic(articleId, id));
        });
        if (!articleTopicList.isEmpty()) {
            articleTopicService.saveBatch(articleTopicList);
        }
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        ConcurrentHashMap<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    @Override
    public Map<Long, ArticleBriefDTO> getArticleBriefByIds(Set<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return Map.of();
        }
        List<ArticleBriefDTO> articleBriefList = articleMapper.getArticleBriefByIds(articleIds);
        if (articleBriefList.isEmpty()) {
            return Map.of();
        }
        return articleBriefList.stream()
                .collect(Collectors.toMap(ArticleBriefDTO::getId, Function.identity()));
    }

    @Override
    public void validateArticleId(Long articleId) {
        Article article = this.getById(articleId);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, ARTICLE_NOT_FOUND);
        }
    }

    @Override
    public void validateArticleId(Long articleId, Long userId) {
        Article article = this.getById(articleId);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, ARTICLE_NOT_FOUND);
        }
        if (Objects.equals(article.getAuthorId(), userId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, MANAGE_OWN_ARTICLE_NOT_ALLOWED);
        }
    }

    @Override
    public List<OperationResult> getBatchOperationResult(List<Operation> operationList, Long userId) {
        List<Long> repostCountQueryArticleIds = new ArrayList<>();
        List<Long> collectionBaseQueryArticleIds = new ArrayList<>();
        operationList.forEach(operation -> {
            if (operation == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, PARAM_EMPTY);
            }
            if (operation.getOperationName() == OperationNameEnum.RepostCountQuery) {
                repostCountQueryArticleIds.add(operation.getId());
            }
            if (operation.getOperationName() == OperationNameEnum.CollectionBaseQuery) {
                collectionBaseQueryArticleIds.add(operation.getId());
            }

        });
        Map<Long, Integer> idRepostCountMap = repostService.getRepostCountMapByArticleIds(repostCountQueryArticleIds);
        Map<Long, Boolean> idIsCollectedMap = collectionListService.isArticlesCollected(collectionBaseQueryArticleIds, userId);
        return operationList.stream()
                .map(operation -> {
                    Long articleId = operation.getId();
                    ArticleResult articleResult = new ArticleResult();
                    articleResult.setId(articleId);
                    if (operation.getOperationName() == OperationNameEnum.RepostCountQuery) {
                        articleResult.setRepostCount(idRepostCountMap.getOrDefault(articleId, 0));
                    }
                    if (operation.getOperationName() == OperationNameEnum.CollectionBaseQuery) {
                        CollectionsConnection collectionsConnection = new CollectionsConnection();
                        collectionsConnection.setHasCollected(idIsCollectedMap.getOrDefault(articleId, false));

                        ArticleViewerEdge articleViewerEdge = new ArticleViewerEdge();
                        articleViewerEdge.setId("articleId:" + articleId + "-viewerId:" + userId);
                        articleViewerEdge.setCollectionsConnection(collectionsConnection);

                        articleResult.setViewerEdge(articleViewerEdge);
                    }

                    OperationResult operationResult = new OperationResult();
                    operationResult.setArticleResult(articleResult);
                    return operationResult;
                }).toList();
    }

    @Override
    public void pinArticleByCreator(Long articleId, Long authorId) {
        Article article = this.getById(articleId);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, ARTICLE_NOT_FOUND);
        }
        if (!Objects.equals(article.getAuthorId(), authorId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, NO_AUTH_EDIT_ARTICLE);
        }

        LambdaQueryWrapper<Article> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(Article::getAuthorId, authorId);
        countWrapper.eq(Article::getIsDelete, 0);
        countWrapper.ne(Article::getPinnedByCreatorAt, 0);
        long pinnedCount = this.count(countWrapper);
        if (pinnedCount >= 5) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, MAXIMUM_NUM_OF_PINNED_ARTICLE);
        }

        long nowTs = System.currentTimeMillis();
        LambdaUpdateWrapper<Article> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Article::getId, articleId);
        updateWrapper.eq(Article::getPinnedByCreatorAt, 0L);
        updateWrapper.set(Article::getPinnedByCreatorAt, nowTs);
        boolean updated = this.update(updateWrapper);
        if (!updated) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, PIN_ARTICLE_MULTIPLE_TIMES);
        }
    }

    @Override
    public void unpinArticleByCreator(Long articleId, Long authorId) {
        Article article = this.getById(articleId);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, ARTICLE_NOT_FOUND);
        }
        if (!Objects.equals(article.getAuthorId(), authorId)) {
            throw new BusinessException(ErrorCode.NO_AUTH, NO_AUTH_EDIT_ARTICLE);
        }

        LambdaUpdateWrapper<Article> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Article::getId, articleId);
        updateWrapper.ne(Article::getPinnedByCreatorAt, 0L);
        updateWrapper.set(Article::getPinnedByCreatorAt, 0L);
        boolean updated = this.update(updateWrapper);
        if (!updated) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, ARTICLE_NOT_PINNED);
        }
    }
}
