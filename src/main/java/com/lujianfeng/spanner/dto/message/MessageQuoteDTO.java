package com.lujianfeng.spanner.dto.message;

import lombok.Data;

@Data
public class MessageQuoteDTO {
    private String messageId;
    private String from;
    private String formName;
    private String fromName;
    private String fromRealName;
    private String fromAvatarUrl;
    private String content;
}
