package com.lujianfeng.spanner.vo.group;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GroupInfoVO {
    private String groupNo;
    private String groupName;
    private String ownerAccount;
    private String announcement;
    private Integer maxMembers;
    private Long memberCount;
    private String myRole;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
