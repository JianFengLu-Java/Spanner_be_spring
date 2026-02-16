package com.lujianfeng.spanner.vo.message;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 私聊消息下行体
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivateMessageVO {
    private String messageId;
    private String from;
    private String to;
    private String content;
    private String clientMessageId;
    private LocalDateTime sentAt;
}
