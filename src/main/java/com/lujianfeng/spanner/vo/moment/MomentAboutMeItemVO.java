package com.lujianfeng.spanner.vo.moment;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class MomentAboutMeItemVO {
    private String id;
    private String type;
    private String momentId;
    private String momentTitle;
    private String sourceCommentId;
    private String parentCommentId;
    private MomentUserVO fromUser;
    private String content;
    private String targetContent;
    private Instant timestamp;
    private Instant createdAt;
}
