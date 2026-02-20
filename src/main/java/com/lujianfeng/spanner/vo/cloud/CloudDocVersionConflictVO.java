package com.lujianfeng.spanner.vo.cloud;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CloudDocVersionConflictVO {
    private Long latestVersion;
    private String latestUpdatedAt;
}
