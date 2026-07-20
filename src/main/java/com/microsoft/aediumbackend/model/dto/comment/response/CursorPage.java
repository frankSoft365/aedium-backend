package com.microsoft.aediumbackend.model.dto.comment.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CursorPage<T> {
    private List<T> items;
    private boolean hasMore;
    private LocalDateTime nextCursorCreatedAt;
    private Long nextCursorId;
}
