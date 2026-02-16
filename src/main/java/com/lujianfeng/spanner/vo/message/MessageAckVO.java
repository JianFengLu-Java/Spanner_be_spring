package com.lujianfeng.spanner.vo.message;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 消息发送回执
 */
@Getter
@Builder
public class MessageAckVO {
    private String clientMessageId;
    private String messageId;
    private String to;
    private String status;
    private LocalDateTime ackAt;
}
