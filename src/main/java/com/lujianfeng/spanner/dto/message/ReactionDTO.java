package com.lujianfeng.spanner.dto.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReactionDTO {
    private String key;
    private String emoji;
    private String imageUrl;
}
