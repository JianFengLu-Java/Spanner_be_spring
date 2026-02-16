package com.lujianfeng.spanner.vo.moment;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class MomentLikeUserVO {
    private String account;
    private String name;
    private String avatar;
    private Instant likedAt;
}
