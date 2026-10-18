package com.microsoft.aediumbackend.service.repost;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.microsoft.aediumbackend.commen.CursorPage;
import com.microsoft.aediumbackend.commen.ErrorCode;
import com.microsoft.aediumbackend.exception.BusinessException;
import com.microsoft.aediumbackend.mapper.UserRepostMapper;
import com.microsoft.aediumbackend.model.dto.article.response.ArticlePreview;
import com.microsoft.aediumbackend.model.dto.repost.request.RepostRequest;
import com.microsoft.aediumbackend.model.dto.repost.request.UserRepostListQuery;
import com.microsoft.aediumbackend.model.dto.repost.response.Repost;
import com.microsoft.aediumbackend.model.dto.repost.response.RepostCount;
import com.microsoft.aediumbackend.model.dto.repost.response.RepostListItem;
import com.microsoft.aediumbackend.model.dto.repost.response.RepostState;
import com.microsoft.aediumbackend.model.dto.user.response.UserInfo;
import com.microsoft.aediumbackend.model.entity.UserRepost;
import com.microsoft.aediumbackend.service.ArticleService;
import com.microsoft.aediumbackend.service.UserService;
import com.microsoft.aediumbackend.utils.CursorPageUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.REPOST_MULTIPLE_TIMES;
import static com.microsoft.aediumbackend.constant.ErrorDescriptionConstant.REPOST_NOT_FOUND;

@Service
public class RepostService extends ServiceImpl<UserRepostMapper, UserRepost> {

    private final UserRepostMapper userRepostMapper;
    private final ArticleService articleService;
    private final UserService userService;

    public RepostService(UserRepostMapper userRepostMapper, ArticleService articleService, UserService userService) {
        this.userRepostMapper = userRepostMapper;
        this.articleService = articleService;
        this.userService = userService;
    }

    public Map<Long, Integer> getRepostCountMapByArticleIds(
            List<Long> articleIds
    ) {
        if (articleIds == null || articleIds.isEmpty()) {
            return new HashMap<>();
        }
        List<RepostCount> repostCounts = userRepostMapper.batchQueryRepostCountByArticleIds(articleIds);
        return repostCounts.stream()
                .collect(Collectors.toMap(
                        RepostCount::getId,
                        RepostCount::getRepostCount
                ));

    }

    // 保证articleId顺序
    public List<RepostCount> getRepostCountByArticleIds(
            List<Long> articleIds
    ) {
        if (articleIds == null || articleIds.isEmpty()) {
            return List.of();
        }
        List<RepostCount> repostCounts = userRepostMapper.batchQueryRepostCountByArticleIds(articleIds);
        Map<Long, Integer> idRepostCountMap = repostCounts.stream()
                .collect(Collectors.toMap(
                        RepostCount::getId,
                        RepostCount::getRepostCount
                ));
        return articleIds.stream()
                .map(id ->
                        new RepostCount(
                                id,
                                idRepostCountMap.getOrDefault(id, 0)
                        ))
                .toList();
    }

    public RepostState getRepostStateForUser(Long userId, Long articleId) {
        LambdaQueryWrapper<UserRepost> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserRepost::getUserId, userId);
        queryWrapper.eq(UserRepost::getArticleId, articleId);
        UserRepost userRepost = this.getOne(queryWrapper);
        if (userRepost == null) {
            return new RepostState(userId, articleId, false, null);
        }
        return new RepostState(userId, articleId, true, userRepost.getNote());
    }

    public void repostAnArticle(Long userId, RepostRequest req) {
        UserRepost userRepost = new UserRepost();
        userRepost.setUserId(userId);
        Long articleId = req.getArticleId();
        articleService.validateArticleId(articleId, userId);
        userRepost.setArticleId(articleId);
        String note = req.getNote();
        if (note == null || note.isBlank()) {
            userRepost.setNote(null);
        } else {
            userRepost.setNote(note.trim());
        }
        try {
            this.save(userRepost);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, REPOST_MULTIPLE_TIMES);
        }
    }

    public void deleteRepost(Long userId, Long articleId) {
        LambdaQueryWrapper<UserRepost> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserRepost::getUserId, userId);
        queryWrapper.eq(UserRepost::getArticleId, articleId);
        boolean remove = this.remove(queryWrapper);
        if (!remove) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, REPOST_NOT_FOUND);
        }
    }

    public void addNote(Long userId, Long articleId, String note) {
        LambdaUpdateWrapper<UserRepost> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserRepost::getUserId, userId)
                .eq(UserRepost::getArticleId, articleId)
                .set(UserRepost::getNote, note.trim())
                .set(UserRepost::getCreateTime, LocalDateTime.now());
        boolean success = this.update(updateWrapper);
        if (!success) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, REPOST_NOT_FOUND);
        }
    }

    public void editNote(Long userId, Long articleId, String note) {
        LambdaUpdateWrapper<UserRepost> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserRepost::getUserId, userId)
                .eq(UserRepost::getArticleId, articleId)
                .set(UserRepost::getNote, note.trim().isBlank() ? null : note.trim());
        boolean success = this.update(updateWrapper);
        if (!success) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, REPOST_NOT_FOUND);
        }
    }

    public void removeNote(Long userId, Long articleId) {
        LambdaUpdateWrapper<UserRepost> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserRepost::getUserId, userId)
                .eq(UserRepost::getArticleId, articleId)
                .set(UserRepost::getNote, null);
        boolean success = this.update(updateWrapper);
        if (!success) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, REPOST_NOT_FOUND);
        }
    }

    public CursorPage<RepostListItem> getRepostList(UserRepostListQuery query) {
        Long userId = query.getUserId();
        UserInfo userInfo = userService.getUserInfoByUserId(userId);

        List<UserRepost> cursorList = userRepostMapper.findUserRepostCursor(
                userId,
                query.getLastCreatedAt(),
                query.getLastId(),
                query.getSize() + 1
        );
        if (cursorList.isEmpty()) {
            return new CursorPage<>(Collections.emptyList(), false, null, null);
        }
        CursorPageUtils.CursorInfo cursorInfo = CursorPageUtils.extract(
                cursorList,
                query.getSize(),
                UserRepost::getCreateTime,
                UserRepost::getArticleId
        );

        List<Long> articleIds = cursorList.stream()
                .map(UserRepost::getArticleId)
                .toList();
        Map<Long, ArticlePreview> idArticleMap = articleService.getArticlePreviewByIds(articleIds, false);
        List<RepostListItem> finalList = cursorList.stream()
                .map(userRepost -> {
                    RepostListItem repostListItem = new RepostListItem();
                    repostListItem.setRepost(Repost.toRepost(userRepost, userInfo));
                    repostListItem.setPost(
                            idArticleMap.getOrDefault(
                                    userRepost.getArticleId(),
                                    ArticlePreview.getDeletedUnavailableArticlePreview(userRepost.getArticleId())
                            )
                    );
                    return repostListItem;
                }).toList();

        return new CursorPage<>(
                finalList,
                cursorInfo.isHasMore(),
                cursorInfo.getNextCursorCreatedAt(),
                cursorInfo.getNextCursorId()
        );
    }
}
