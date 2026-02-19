package com.lujianfeng.spanner.dto.group;

import lombok.Data;

@Data
public class GroupProfileUpdateRequestDTO {
    private String groupName;
    private String groupAvatarUrl;
    private String summary;
}
