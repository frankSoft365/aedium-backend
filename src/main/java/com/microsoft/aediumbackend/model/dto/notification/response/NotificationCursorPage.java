package com.microsoft.aediumbackend.model.dto.notification.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationCursorPage<T> {
    private List<T> items;
    private Long watermark;
    private boolean hasMore;
    private LocalDateTime nextCursorCreatedAt;
    private Long nextCursorId;
}
