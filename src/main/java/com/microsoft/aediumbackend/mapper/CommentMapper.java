package com.microsoft.aediumbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.microsoft.aediumbackend.model.dto.comment.response.ArticleCommentCount;
import com.microsoft.aediumbackend.model.dto.comment.response.CommentBriefDTO;
import com.microsoft.aediumbackend.model.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    List<Comment> findRootCommentsCursor(Long articleId, LocalDateTime lastCreatedAt, Long lastId, int size);

    List<Comment> findReplyPreviewsForRoots(List<Long> rootIds, int previewSize);

    List<Comment> findReplyPreviewsContextForRootByReplyId(List<Long> ids);

    @Update("update comment set reply_count=reply_count+1 where id=#{rootId}")
    void incrementReplyCount(Long rootId);

    @Update("UPDATE comment SET like_count = like_count + 1 WHERE id = #{id}")
    int incrementLikeCount(@Param("id") Long id);

    @Update("UPDATE comment SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{id}")
    int decrementLikeCount(@Param("id") Long id);

    List<Comment> findRepliesForRoot(Long articleId, Long rootId, LocalDateTime lastCreatedAt, Long lastId, int size);

    /**
     * 根据文章id集合获取评论
     */
    List<ArticleCommentCount> findCommentCountForArticleIds(List<Long> articleIds);

    /**
     * 根据ID列表获取评论简要信息
     */
    List<CommentBriefDTO> getCommentBriefByIds(Set<Long> commentIds);

}
