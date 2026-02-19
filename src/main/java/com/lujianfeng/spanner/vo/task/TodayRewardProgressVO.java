package com.lujianfeng.spanner.vo.task;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TodayRewardProgressVO {
    private Long userId;
    private String date;
    private String timezone;
    private Integer postGrantedCount;
    private Integer postDailyLimit;
    private Integer postRemainingCount;
}
