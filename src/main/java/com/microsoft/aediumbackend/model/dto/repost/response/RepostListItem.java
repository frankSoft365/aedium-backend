package com.microsoft.aediumbackend.model.dto.repost.response;

import com.microsoft.aediumbackend.model.dto.article.response.ArticlePreview;
import com.microsoft.aediumbackend.model.entity.UserRepost;
import lombok.Data;

@Data
public class RepostListItem {
    private UserRepost repost;
    private ArticlePreview post;
}
