package com.microsoft.aediumbackend.model.dto.repost.response;

import com.microsoft.aediumbackend.model.dto.user.response.UserInfo;
import com.microsoft.aediumbackend.model.entity.UserRepost;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Repost {
    private String id;
    private String note;
    private LocalDateTime repostedAt;
    private UserInfo user;

    public static Repost toRepost(UserRepost userRepost, UserInfo userInfo) {
        Repost repost = new Repost();
        repost.setId(userRepost.getUserId() + ":" + userRepost.getArticleId());
        repost.setNote(userRepost.getNote());
        repost.setRepostedAt(userRepost.getCreateTime());
        repost.setUser(userInfo);
        return repost;
    }
}
