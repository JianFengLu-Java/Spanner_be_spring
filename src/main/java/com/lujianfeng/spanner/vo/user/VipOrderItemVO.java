package com.lujianfeng.spanner.vo.user;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class VipOrderItemVO {

    private String purchaseNo;

    private String planCode;

    private String planName;

    private BigDecimal amount;

    private Integer months;

    private Long growthBonus;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private String status;

    private LocalDateTime createdAt;
}
