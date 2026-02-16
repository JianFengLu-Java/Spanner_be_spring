package com.lujianfeng.spanner.vo.moment;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class MomentCommentItemVO {
    private String id;
    private String momentId;
    private String parentCommentId;
    private String replyToAccount;
    private MomentUserVO author;
    private String text;
    private Long likes;
    private Boolean isLiked;
    private Long replyCount;
    private Instant timestamp;
    private Instant createdAt;
}
