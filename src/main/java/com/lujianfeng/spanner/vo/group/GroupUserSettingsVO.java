package com.lujianfeng.spanner.vo.group;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GroupUserSettingsVO {
    private String groupNo;
    private Boolean messageMute;
    private Boolean chatPinned;
    private Boolean saveToContacts;
    private LocalDateTime updatedAt;
}
