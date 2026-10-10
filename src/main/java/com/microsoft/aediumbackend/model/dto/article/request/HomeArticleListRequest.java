package com.microsoft.aediumbackend.model.dto.article.request;

import com.microsoft.aediumbackend.commen.CursorPageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class HomeArticleListRequest extends CursorPageRequest {
}
