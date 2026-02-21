package com.lujianfeng.spanner.vo.cloud;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CloudDocWsPatchDataVO {
    private Long baseVersion;
    private Long serverVersion;
    private String opId;
    private String opType;
    private String payload;
}
