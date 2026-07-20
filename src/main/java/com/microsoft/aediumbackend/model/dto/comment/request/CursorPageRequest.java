package com.microsoft.aediumbackend.model.dto.comment.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CursorPageRequest {
    private LocalDateTime lastCreatedAt;
    
    private Long lastId;
    
    @Min(value = 1, message = "size最小为1")
    @Max(value = 50, message = "size最大为50")
    private int size = 12;
}
