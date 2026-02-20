package com.lujianfeng.spanner.vo.cloud;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CloudDocShareCreateResultVO {
    private String shareNo;
    private String docId;
    private String friendAccount;
    private String shareMode;
    private String createdAt;
    private String expireAt;
    private String sharePath;
}
