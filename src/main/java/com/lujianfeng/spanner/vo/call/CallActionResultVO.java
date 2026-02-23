package com.lujianfeng.spanner.vo.call;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CallActionResultVO {
    private String callId;
    private String status;
    private String answeredAt;
    private String endedAt;
    private String endReason;
    private Long durationSeconds;
    private String expiresAt;
}
