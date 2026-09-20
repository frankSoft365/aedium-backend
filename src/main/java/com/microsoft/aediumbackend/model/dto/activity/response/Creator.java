package com.microsoft.aediumbackend.model.dto.activity.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Creator {
    private Long id;
    private String avatar;
    private String username;

    public Creator(Long id, String username) {
        this.id = id;
        this.username = username;
    }
}
