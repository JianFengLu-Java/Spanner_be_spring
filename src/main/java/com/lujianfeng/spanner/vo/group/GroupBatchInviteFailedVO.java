package com.lujianfeng.spanner.vo.group;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GroupBatchInviteFailedVO {
    private String account;
    private String reasonCode;
    private String reason;
}
