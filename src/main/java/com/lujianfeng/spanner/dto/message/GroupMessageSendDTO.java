package com.lujianfeng.spanner.dto.message;

import lombok.Data;

@Data
public class GroupMessageSendDTO {
    private String groupNo;
    private String content;
    private String clientMessageId;
}
