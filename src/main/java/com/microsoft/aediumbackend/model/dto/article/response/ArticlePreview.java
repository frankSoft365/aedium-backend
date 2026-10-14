package com.microsoft.aediumbackend.model.dto.article.response;

import com.microsoft.aediumbackend.model.dto.activity.response.Creator;
import com.microsoft.aediumbackend.model.dto.activity.response.ImageMetaData;
import com.microsoft.aediumbackend.model.dto.activity.response.PreviewContent;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticlePreview {
    // 文章id
    private Long id;
    // 作者
    private Creator creator;
    // 标题
    private String title;
    // 副标题 仅副标题不为空时extendedPreviewContent存在
    private PreviewContent extendedPreviewContent;
    // 封面图
    private ImageMetaData previewImage;
    // 文章发布时间
    private LocalDateTime publishAt;

    // 点赞数
    private Integer clapCount;
    // 评论数
    private Integer responseCount;

    public static ArticlePreview getDeletedUnavailableArticlePreview(Long id) {
        ArticlePreview articlePreview = new ArticlePreview();
        articlePreview.setId(id);
        return articlePreview;
    }

    public static ArticlePreview toArticlePreview(ArticleWithAuthor article) {
        ArticlePreview articlePreview = new ArticlePreview();
        articlePreview.setId(article.getId());
        articlePreview.setCreator(
                new Creator(
                        article.getAuthorId(),
                        article.getAuthorAvatar(),
                        article.getAuthorName()
                )
        );
        articlePreview.setTitle(article.getTitle());
        // 只有副标题不为空才算副标题
        String subtitle = article.getSubtitle();
        boolean isFullContent = subtitle == null || subtitle.isBlank();
        articlePreview.setExtendedPreviewContent(
                new PreviewContent(
                        isFullContent,
                        isFullContent ? null : subtitle
                )
        );

        articlePreview.setPreviewImage(
                new ImageMetaData(
                        article.getCoverImage(),
                        article.getCoverFocusY()
                )
        );
        articlePreview.setPublishAt(article.getPublishTime());
        articlePreview.setClapCount(article.getLikeCount());
        return articlePreview;
    }
}
