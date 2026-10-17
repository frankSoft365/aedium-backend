package com.microsoft.aediumbackend.model.dto.follow.response;

import lombok.Data;

@Data
public class User {
    // 用户id
    private Long id;
    // 观察者数据
    private UserViewerEdge viewerEdge;
}
