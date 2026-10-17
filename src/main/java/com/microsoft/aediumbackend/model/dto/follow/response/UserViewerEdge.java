package com.microsoft.aediumbackend.model.dto.follow.response;

import lombok.Data;

@Data
public class UserViewerEdge {
    /**
     * viewerEdge id
     * 例子：userId:505daad3422d-viewerId:3d34188467c3
     * userId 用户观察的用户id
     * viewerId 作为观察者的用户id
     */
    private String id;

    /**
     * 用户是否关注了当前用户
     */
    private Boolean isFollowing;

}
