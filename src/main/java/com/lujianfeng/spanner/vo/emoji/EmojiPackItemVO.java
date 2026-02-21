package com.lujianfeng.spanner.vo.emoji;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class EmojiPackItemVO {
    private String id;
    private String displayName;
    private String imageUrl;
    private String objectName;
    private String contentType;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private Long favoriteCount;
    private Boolean favorited;
    private Instant createdAt;
}
