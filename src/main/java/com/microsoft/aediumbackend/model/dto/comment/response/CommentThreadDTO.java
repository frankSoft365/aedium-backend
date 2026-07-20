package com.microsoft.aediumbackend.model.dto.comment.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentThreadDTO {
    private CommentView root;
    private List<CommentView> replyPreview;
    private int totalReplyCount;
    private boolean hasMoreReplies;
}
