package com.lujianfeng.spanner.vo.message;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class ReactionItemVO {
    private String key;
    private String emoji;
    private String imageUrl;
    private Long count;
    private List<String> userIds;
    private Instant updatedAt;
}
