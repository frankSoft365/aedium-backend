package com.microsoft.aediumbackend.model.dto.article;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArticlePublishRequest {
    @NotBlank(message = "title不能为空")
    private String title;

    private String subtitle;

    @NotBlank(message = "content不能为空")
    private String content;

    private String coverImage;

    @NotNull(message = "topics不能为null")
    private List<String> topics;

    @NotBlank(message = "发布状态不能为空")
    private String status;

    private LocalDateTime publishAt;
}
