package com.lujianfeng.spanner.vo.user;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class VipProfileVO {

    private Boolean vipActive;

    private LocalDateTime vipExpireAt;

    private Long growthValue;

    private Integer userLevel;

    private Long nextLevelGrowth;
}
