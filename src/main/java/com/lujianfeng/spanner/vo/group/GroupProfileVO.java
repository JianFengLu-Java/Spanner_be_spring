package com.lujianfeng.spanner.vo.group;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GroupProfileVO {
    private String groupNo;
    private String groupName;
    private String groupAvatarUrl;
    private String summary;
    private String ownerAccount;
    private String myRole;
    private Long memberCount;
    private Integer maxMembers;
    private String inviteMode;
    private Boolean memberCanEditGroupName;
    private Boolean joinVerificationEnabled;
    private String announcementPermission;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
