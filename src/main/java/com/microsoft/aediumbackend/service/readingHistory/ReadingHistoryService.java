package com.microsoft.aediumbackend.service.readingHistory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.microsoft.aediumbackend.commen.CursorPage;
import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.exception.BusinessException;
import com.microsoft.aediumbackend.mapper.UserReadingHistoryMapper;
import com.microsoft.aediumbackend.model.dto.readingHistory.request.ReadingHistoryQuery;
import com.microsoft.aediumbackend.model.entity.UserReadingHistory;
import com.microsoft.aediumbackend.model.vo.ArticleListItemVO;
import com.microsoft.aediumbackend.service.ArticleService;
import com.microsoft.aediumbackend.utils.CursorPageUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.READING_HISTORY_NOT_FOUND;

/**
 * 用户阅读历史服务
 */
@Service
public class ReadingHistoryService extends ServiceImpl<UserReadingHistoryMapper, UserReadingHistory> {
    private final UserReadingHistoryMapper userReadingHistoryMapper;
    private final ArticleService articleService;

    public ReadingHistoryService(UserReadingHistoryMapper userReadingHistoryMapper, ArticleService articleService) {
        this.userReadingHistoryMapper = userReadingHistoryMapper;
        this.articleService = articleService;
    }

    /**
     * 记录一条阅读历史
     */
    public void saveReadingHistory(Long userId, Long articleId) {
        // 必须保证存的文章id是有效的
        articleService.validateArticleId(articleId);
        userReadingHistoryMapper.insertOrUpdateReadHistory(userId, articleId);
    }

    /**
     * 获取游标控制的文章预览列表
     */
    public CursorPage<ArticleListItemVO> getReadingHistoryList(Long userId, ReadingHistoryQuery query) {
        List<UserReadingHistory> userReadingHistoryList = userReadingHistoryMapper.findUserReadingHistoryCursor(
                userId,
                query.getLastCreatedAt(),
                query.getLastId(),
                query.getSize() + 1
        );
        if (userReadingHistoryList.isEmpty()) {
            return new CursorPage<>(Collections.emptyList(), false, null, null);
        }
        CursorPageUtils.CursorInfo cursorInfo = CursorPageUtils.extract(
                userReadingHistoryList,
                query.getSize(),
                UserReadingHistory::getLastReadingTime,
                UserReadingHistory::getArticleId
        );
        List<Long> articleIds = userReadingHistoryList.stream()
                .map(UserReadingHistory::getArticleId)
                .toList();
        // 如果原文章被作者删除，将不包含在list中
        Map<Long, ArticleListItemVO> idArticleMap = articleService.getArticleListItemVOByIds(articleIds);
        List<ArticleListItemVO> list = articleIds.stream()
                .map(articleId ->
                        idArticleMap.getOrDefault(
                                articleId,
                                new ArticleListItemVO(articleId)
                        ))
                .toList();

        return new CursorPage<>(
                list,
                cursorInfo.isHasMore(),
                cursorInfo.getNextCursorCreatedAt(),
                cursorInfo.getNextCursorId()
        );
    }

    /**
     * 清空用户的所有阅读历史
     */
    public void clearAllReadingHistory(Long userId) {
        LambdaQueryWrapper<UserReadingHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserReadingHistory::getUserId, userId);
        boolean remove = this.remove(queryWrapper);
        if (!remove) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, READING_HISTORY_NOT_FOUND);
        }
    }

    /**
     * 删除某个指定的文章的阅读历史记录
     */
    public void deleteOneReadingHistory(Long userId, Long articleId) {
        LambdaQueryWrapper<UserReadingHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserReadingHistory::getUserId, userId);
        queryWrapper.eq(UserReadingHistory::getArticleId, articleId);
        boolean remove = this.remove(queryWrapper);
        if (!remove) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, READING_HISTORY_NOT_FOUND);
        }
    }
}
