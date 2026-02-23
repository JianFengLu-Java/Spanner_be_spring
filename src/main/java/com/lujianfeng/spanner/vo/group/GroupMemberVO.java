package com.lujianfeng.spanner.vo.group;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GroupMemberVO {
    private String account;
    private String role;
    @JsonProperty("isVip")
    private Boolean isVip;
    private LocalDateTime joinedAt;
}
