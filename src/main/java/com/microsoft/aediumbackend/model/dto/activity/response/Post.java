package com.microsoft.aediumbackend.model.dto.activity.response;

import com.microsoft.aediumbackend.model.dto.article.response.ArticlePreview;
import com.microsoft.aediumbackend.model.dto.comment.response.CommentBriefDTO;
import com.microsoft.aediumbackend.model.dto.user.response.UserBriefDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    private Long id;
    private Integer clapCount;
    private Integer responseCount;
    private ImageMetaData previewImage;
    private String title;
    private PreviewContent extendedPreviewContent;
    private String content;
    private Creator creator;
    private LocalDateTime publishAt;

    // 用户回复的对象 文章或者评论
    private ResponseRootPost responseRootPost;

    public static Post buildForArticle(
            ArticlePreview article
    ) {
        Post post = new Post();
        post.setId(article.getId());
        post.setClapCount(article.getClapCount());
        post.setResponseCount(article.getResponseCount());
        post.setPreviewImage(article.getPreviewImage());
        post.setTitle(article.getTitle());
        post.setExtendedPreviewContent(article.getExtendedPreviewContent());
        post.setCreator(article.getCreator());
        post.setPublishAt(article.getPublishAt());
        post.setResponseRootPost(new ResponseRootPost(
                buildForResponseRootPost(article)
        ));
        return post;
    }

    private static Post buildForResponseRootPost(ArticlePreview article) {
        Post post = new Post();
        post.setId(article.getId());
        post.setClapCount(article.getClapCount());
        post.setResponseCount(article.getResponseCount());
        post.setPreviewImage(article.getPreviewImage());
        post.setTitle(article.getTitle());
        post.setExtendedPreviewContent(article.getExtendedPreviewContent());
        post.setCreator(article.getCreator());
        post.setPublishAt(article.getPublishAt());
        return post;
    }

    public static Post buildForComment(CommentBriefDTO comment, UserBriefDTO user) {
        Post post = new Post();
        post.setId(comment.getId());
        post.setContent(comment.getContent());
        post.setCreator(new Creator(
                user.getId(),
                user.getImage(),
                user.getUsername()
        ));
        post.setPublishAt(comment.getCreateTime());
        return post;
    }

    public static Post buildForParentComment(CommentBriefDTO comment, UserBriefDTO user, ArticlePreview article) {
        Post post = new Post();
        post.setId(comment.getId());
        post.setContent(comment.getContent());
        post.setCreator(new Creator(
                user.getId(),
                user.getImage(),
                user.getUsername()
        ));
        post.setPublishAt(comment.getCreateTime());
        post.setExtendedPreviewContent(new PreviewContent(
                true,
                ""
        ));
        // 关联的文章信息
        post.setResponseRootPost(new ResponseRootPost(
                buildForArticle(article)
        ));
        return post;
    }
}
