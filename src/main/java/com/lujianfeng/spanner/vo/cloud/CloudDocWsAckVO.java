package com.lujianfeng.spanner.vo.cloud;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CloudDocWsAckVO {
    private String action;
    private String docId;
    private String status;
    private String at;
}
