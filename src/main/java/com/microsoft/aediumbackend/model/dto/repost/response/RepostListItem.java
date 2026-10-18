package com.microsoft.aediumbackend.model.dto.repost.response;

import com.microsoft.aediumbackend.model.dto.article.response.ArticlePreview;
import lombok.Data;

@Data
public class RepostListItem {
    private Repost repost;
    private ArticlePreview post;
}
