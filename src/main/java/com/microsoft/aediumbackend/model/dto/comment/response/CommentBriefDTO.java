package com.microsoft.aediumbackend.model.dto.comment.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentBriefDTO {
    private Long id;
    private Long userId;
    private String username;
    private String content;
    private Long parentId;
    private Long replyToUserId;
    private String replyToUsername;
}
