package com.lujianfeng.spanner.dto.cloud;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CloudDocWsPatchDTO {
    private String docId;
    private Long baseVersion;
    private String opId;
    private String opType;
    private String payload;
}
