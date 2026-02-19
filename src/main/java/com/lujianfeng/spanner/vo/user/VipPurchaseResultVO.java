package com.lujianfeng.spanner.vo.user;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class VipPurchaseResultVO {

    private String purchaseNo;

    private String paymentOrderNo;

    private String paymentMethod;

    private String planCode;

    private String planName;

    private BigDecimal amount;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private Boolean vipActive;

    private LocalDateTime vipExpireAt;

    private Long growthValue;

    private Integer userLevel;
}
