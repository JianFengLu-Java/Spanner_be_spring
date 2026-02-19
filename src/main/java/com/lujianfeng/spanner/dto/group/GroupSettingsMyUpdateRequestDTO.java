package com.lujianfeng.spanner.dto.group;

import lombok.Data;

@Data
public class GroupSettingsMyUpdateRequestDTO {
    private Boolean messageMute;
    private Boolean chatPinned;
    private Boolean saveToContacts;
}
