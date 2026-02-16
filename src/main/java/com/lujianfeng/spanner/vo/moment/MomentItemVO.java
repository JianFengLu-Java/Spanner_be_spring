package com.lujianfeng.spanner.vo.moment;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class MomentItemVO {
    private String id;
    private String title;
    private String cover;
    private MomentUserVO author;
    private String content;
    private String contentHtml;
    private List<String> images;
    private Long likes;
    private Boolean isLiked;
    private List<MomentUserVO> likePreviewUsers;
    private Long commentsCount;
    private Boolean isFavorited;
    private String friendStatusWithAuthor;
    private Instant timestamp;
    private Instant createdAt;
    private Instant updatedAt;
}
