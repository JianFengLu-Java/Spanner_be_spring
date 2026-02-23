package com.lujianfeng.spanner.dto.call;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CallSignalRequestDTO {
    private String requestId;
    private String signalType;
    private String to;
    private String sdp;
    private String candidate;
    private String sdpMid;
    private Integer sdpMLineIndex;
}
