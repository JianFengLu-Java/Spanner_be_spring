package com.lujianfeng.spanner.vo.user;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class VipPlanVO {

    private String planCode;

    private String planName;

    private BigDecimal price;

    private Integer months;

    private Long growthBonus;
}
