package com.lujianfeng.spanner.vo.task;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TaskEventProcessResultVO {
    private String eventId;
    private String taskType;
    private String grantStatus;
    private Integer rewardWalletCent;
    private Integer rewardGrowth;
    private Integer todayGrantedCount;
    private Integer todayRemainingCount;
    private String reason;
    private boolean duplicate;
}
