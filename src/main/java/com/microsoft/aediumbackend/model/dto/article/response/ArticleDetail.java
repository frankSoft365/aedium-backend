package com.microsoft.aediumbackend.model.dto.article.response;

import com.microsoft.aediumbackend.model.dto.activity.response.Creator;
import com.microsoft.aediumbackend.model.dto.activity.response.ImageMetaData;
import com.microsoft.aediumbackend.model.dto.activity.response.PreviewContent;
import com.microsoft.aediumbackend.model.vo.TopicInArticleVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ArticleDetail extends ArticlePreview{
    private String content;
    private List<TopicInArticleVO> topics;

    public static ArticleDetail toArticleDetail(ArticleWithAuthor article) {
        ArticleDetail articleDetail = new ArticleDetail();
        articleDetail.setId(article.getId());
        articleDetail.setCreator(
                new Creator(
                        article.getAuthorId(),
                        article.getAuthorAvatar(),
                        article.getAuthorName()
                )
        );
        articleDetail.setTitle(article.getTitle());
        // 只有副标题不为空才算副标题
        String subtitle = article.getSubtitle();
        boolean isFullContent = subtitle == null || subtitle.isBlank();
        articleDetail.setExtendedPreviewContent(
                new PreviewContent(
                        isFullContent,
                        isFullContent ? null : subtitle
                )
        );

        articleDetail.setPreviewImage(
                new ImageMetaData(
                        article.getCoverImage(),
                        article.getCoverFocusY()
                )
        );
        articleDetail.setPublishAt(article.getPublishTime());
        articleDetail.setClapCount(article.getLikeCount());

        articleDetail.setContent(article.getContent());

        return articleDetail;
    }
}
