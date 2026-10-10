package com.microsoft.aediumbackend.model.dto.repost.response;

import com.microsoft.aediumbackend.model.entity.UserRepost;
import com.microsoft.aediumbackend.model.vo.ArticleListItemVO;
import lombok.Data;

@Data
public class RepostListItem {
    private UserRepost repost;
    private ArticleListItemVO post;
}
