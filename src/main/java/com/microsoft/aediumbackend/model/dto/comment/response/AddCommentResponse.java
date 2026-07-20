package com.microsoft.aediumbackend.model.dto.comment.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddCommentResponse {
    private CommentView commentView;
    private Long rootId;
}
