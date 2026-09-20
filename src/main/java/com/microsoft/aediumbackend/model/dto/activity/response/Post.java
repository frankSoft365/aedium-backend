package com.microsoft.aediumbackend.model.dto.activity.response;

import com.microsoft.aediumbackend.model.dto.article.response.ArticleBriefDTO;
import com.microsoft.aediumbackend.model.dto.comment.response.CommentBriefDTO;
import com.microsoft.aediumbackend.model.dto.user.response.UserBriefDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

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
            ArticleBriefDTO article,
            Map<Long, UserBriefDTO> userMap,
            Map<Long, Integer> articleResponseCountMap
    ) {
        Post post = new Post();
        post.setId(article.getId());
        post.setClapCount(article.getLikeCount());
        post.setResponseCount(articleResponseCountMap.getOrDefault(article.getId(), 0));
        post.setPreviewImage(new ImageMetaData(
                article.getCoverImage(),
                article.getCoverFocusY()
        ));
        post.setTitle(article.getTitle());
        post.setExtendedPreviewContent(new PreviewContent(
                false,
                article.getSubtitle()
        ));
        Long authorId = article.getAuthorId();
        UserBriefDTO userBriefDTO = userMap.getOrDefault(authorId, new UserBriefDTO());
        post.setCreator(new Creator(
                authorId,
                userBriefDTO.getImage(),
                userBriefDTO.getUsername()
        ));
        post.setPublishAt(article.getCreateTime());
        post.setResponseRootPost(new ResponseRootPost(
                buildForArticle(article)
        ));
        return post;
    }

    private static Post buildForArticle(ArticleBriefDTO article) {
        Post post = new Post();
        post.setId(article.getId());
        post.setPreviewImage(new ImageMetaData(
                article.getCoverImage(),
                article.getCoverFocusY()
        ));
        post.setTitle(article.getTitle());
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
        return post;
    }

    public static Post buildForParentComment(CommentBriefDTO comment, UserBriefDTO user, ArticleBriefDTO article) {
        Post post = new Post();
        post.setId(comment.getId());
        post.setContent(comment.getContent());
        post.setCreator(new Creator(
                user.getId(),
                user.getImage(),
                user.getUsername()
        ));
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
