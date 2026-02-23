package com.lujianfeng.spanner.vo.call;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CallSessionVO {
    private String callId;
    private String type;
    private String status;
    private String callerAccount;
    private String callerName;
    private String callerAvatar;
    private String calleeAccount;
    private String calleeName;
    private String calleeAvatar;
    private String channelId;
    private String startedAt;
    private String expiresAt;
    private String answeredAt;
    private String connectedAt;
    private String endedAt;
    private String endReason;
    private Long durationSeconds;
}
