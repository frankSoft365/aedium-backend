package com.microsoft.aediumbackend.model.dto.comment.response;

import com.microsoft.aediumbackend.model.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentView {
    private Long id;
    private Long userId;

    private String username;
    private String userAvatar;

    private String content;

    private Long parentId;

    private Long replyToUserId;  // null for root
    private String replyToUsername;

    private LocalDateTime createdAt;

}