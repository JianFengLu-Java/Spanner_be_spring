package com.lujianfeng.spanner.vo.cloud;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CloudDocShareViewVO {
    private String shareNo;
    private String shareMode;
    private Boolean collaborative;
    private CloudDocDetailVO doc;
}
