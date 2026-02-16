package com.lujianfeng.spanner.vo.user;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class WalletChangeResultVO {

    private String changeType;

    private String businessNo;

    private String remark;

    private BigDecimal amount;

    private BigDecimal beforeBalance;

    private BigDecimal afterBalance;

    private LocalDateTime changeTime;

    private WalletAccountVO wallet;
}
