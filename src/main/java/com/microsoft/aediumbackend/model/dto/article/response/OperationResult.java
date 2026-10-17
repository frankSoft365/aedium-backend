package com.microsoft.aediumbackend.model.dto.article.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.microsoft.aediumbackend.model.dto.follow.response.User;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class OperationResult {
    private ArticleResult articleResult;
    private User user;
}
