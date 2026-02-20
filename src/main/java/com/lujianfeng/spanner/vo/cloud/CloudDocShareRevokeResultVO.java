package com.lujianfeng.spanner.vo.cloud;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CloudDocShareRevokeResultVO {
    private String shareNo;
    private Boolean revoked;
    private String revokedAt;
}
