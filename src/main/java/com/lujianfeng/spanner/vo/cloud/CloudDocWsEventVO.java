package com.lujianfeng.spanner.vo.cloud;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CloudDocWsEventVO {
    private String eventType;
    private String docId;
    private String from;
    private String at;
    private Object data;
}
