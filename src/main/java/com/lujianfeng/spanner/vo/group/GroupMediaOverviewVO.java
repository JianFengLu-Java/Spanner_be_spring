package com.lujianfeng.spanner.vo.group;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GroupMediaOverviewVO {
    private String groupNo;
    private Long fileCount;
    private Long imageVideoCount;
    private Long linkCount;
}
