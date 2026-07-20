package com.microsoft.aediumbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.microsoft.aediumbackend.model.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    List<Comment> findRootCommentsCursor(Long articleId, LocalDateTime lastCreatedAt, Long lastId, int size);

    List<Comment> findReplyPreviewsForRoots(List<Long> rootIds, int previewSize);

    @Update("update comment set reply_count=reply_count+1 where id=#{rootId}")
    void incrementReplyCount(Long rootId);

    List<Comment> findRepliesForRoot(Long articleId, Long rootId, LocalDateTime lastCreatedAt, Long lastId, int size);
}
