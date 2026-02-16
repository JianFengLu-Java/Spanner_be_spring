package com.lujianfeng.spanner.vo.message;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * WebSocket 业务错误
 */
@Getter
@Builder
public class WsErrorVO {
    private String code;
    private String message;
    private String clientMessageId;
    private LocalDateTime at;
}
