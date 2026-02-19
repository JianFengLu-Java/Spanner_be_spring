package com.lujianfeng.spanner.dto.group;

import lombok.Data;

@Data
public class GroupSettingsPermissionUpdateRequestDTO {
    private String inviteMode;
    private Boolean memberCanEditGroupName;
    private Boolean joinVerificationEnabled;
    private String announcementPermission;
}
