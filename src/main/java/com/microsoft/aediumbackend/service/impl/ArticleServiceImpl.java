package com.microsoft.aediumbackend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.exception.BusinessException;
import com.microsoft.aediumbackend.mapper.ArticleMapper;
import com.microsoft.aediumbackend.mapper.TopicMapper;
import com.microsoft.aediumbackend.model.dto.article.ArticlePublishRequest;
import com.microsoft.aediumbackend.model.entity.Article;
import com.microsoft.aediumbackend.model.entity.ArticleTopic;
import com.microsoft.aediumbackend.model.entity.Topic;
import com.microsoft.aediumbackend.model.enums.PublishStatusEnum;
import com.microsoft.aediumbackend.model.vo.ArticleVO;
import com.microsoft.aediumbackend.service.ArticleService;
import com.microsoft.aediumbackend.service.ArticleTopicService;
import com.microsoft.aediumbackend.service.TopicService;
import com.microsoft.aediumbackend.utils.CurrentHold;
import com.microsoft.aediumbackend.utils.SlugUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
                handleTopics(topicNames, articleId);
            }
            
            return articleId;
        }
        if (PublishStatusEnum.SCHEDULED.equals(enumByValue)) {

        }
        return null;
    }

    @Override
    public ArticleVO getArticleById(Long id) {
        ArticleVO articleVO = articleMapper.getArticleById(id);
        if (articleVO == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, ARTICLE_NOT_FOUND);
        }
        return articleVO;
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
}

