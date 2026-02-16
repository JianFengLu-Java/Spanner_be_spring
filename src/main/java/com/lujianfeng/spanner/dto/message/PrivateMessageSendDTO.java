package com.lujianfeng.spanner.dto.message;

import lombok.Data;

/**
 * 私聊发送请求
 */
@Data
public class PrivateMessageSendDTO {
    private String to;
    private String content;
    private String clientMessageId;
}
