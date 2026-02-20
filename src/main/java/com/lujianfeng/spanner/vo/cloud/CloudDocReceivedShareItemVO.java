package com.lujianfeng.spanner.vo.cloud;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CloudDocReceivedShareItemVO {
    private String shareNo;
    private String docId;
    private String title;
    private String snippet;
    private String ownerAccount;
    private String shareMode;
    private String status;
    private Boolean expired;
    private String createdAt;
    private String expireAt;
    private String lastViewedAt;
}
