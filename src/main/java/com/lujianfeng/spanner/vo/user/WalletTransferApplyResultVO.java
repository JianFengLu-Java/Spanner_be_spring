package com.lujianfeng.spanner.vo.user;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class WalletTransferApplyResultVO {

    private String businessNo;

    private String toAccount;

    private BigDecimal amount;

    private String remark;

    private String status;

    private LocalDateTime createdAt;
}
