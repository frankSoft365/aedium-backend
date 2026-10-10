package com.microsoft.aediumbackend.model.dto.repost.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepostCountQuery {
    private List<Long> ids;
}
