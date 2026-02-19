package com.lujianfeng.spanner.vo.group;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GroupMemberItemVO {
    private String account;
    private String name;
    private String avatarUrl;
    private String role;
    private String status;
    private LocalDateTime joinedAt;
    private Boolean muted;
    private Boolean blacklisted;
}
