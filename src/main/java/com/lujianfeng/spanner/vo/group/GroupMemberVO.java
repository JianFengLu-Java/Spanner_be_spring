package com.lujianfeng.spanner.vo.group;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GroupMemberVO {
    private String account;
    private String role;
    private LocalDateTime joinedAt;
}
