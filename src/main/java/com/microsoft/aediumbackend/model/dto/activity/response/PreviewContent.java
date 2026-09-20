package com.microsoft.aediumbackend.model.dto.activity.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PreviewContent {
    private Boolean isFullContent;
    private String subtitle;
}
