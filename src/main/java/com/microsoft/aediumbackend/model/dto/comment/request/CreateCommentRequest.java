package com.microsoft.aediumbackend.model.dto.comment.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCommentRequest {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 2000, message = "评论过长")
    private String content;

    /**
     * 回复给id为xxx的评论
     */
    private Long parentId;

}
