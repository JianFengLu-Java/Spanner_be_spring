package com.lujianfeng.spanner.vo.task;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class GrowthLedgerItemVO {
    private String ledgerId;
    private String bizType;
    private String taskType;
    private String bizId;
    private Integer changeGrowth;
    private Long growthAfter;
    private LocalDateTime createdAt;
}
