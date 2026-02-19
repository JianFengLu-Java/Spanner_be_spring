package com.lujianfeng.spanner.vo.task;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TaskConfigVO {
    private String taskType;
    private Boolean enabled;
    private Integer rewardWalletCent;
    private Integer rewardGrowth;
    private Integer dailyLimit;
}
