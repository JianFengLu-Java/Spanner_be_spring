package com.lujianfeng.spanner.vo.cloud;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CloudDocWsAckVO {
    private String action;
    private String docId;
    private String opId;
    private Long baseVersion;
    private Long serverVersion;
    private String status;
    private String reason;
    private String at;
}
