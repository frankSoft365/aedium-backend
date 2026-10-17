package com.microsoft.aediumbackend.model.dto.article.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ArticleSetPinnedByCreatorAtRequest {
    /**
     * pin : isPinned=true
     * unpin : isPinned=false
     */
    @NotNull(message = "isPinned不能为空，true置顶，false取消置顶")
    private Boolean isPinned;
    @NotNull(message = "文章id不能为空")
    @Min(value = 1, message = "文章id必须大于0")
    private Long articleId;
}
