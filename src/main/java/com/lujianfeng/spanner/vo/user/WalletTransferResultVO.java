package com.lujianfeng.spanner.vo.user;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class WalletTransferResultVO {

    private String businessNo;

    private String toAccount;

    private BigDecimal amount;

    private String remark;

    private BigDecimal fromBeforeBalance;

    private BigDecimal fromAfterBalance;

    private BigDecimal toBeforeBalance;

    private BigDecimal toAfterBalance;

    private LocalDateTime changeTime;

    private WalletAccountVO fromWallet;

    private WalletAccountVO toWallet;
}
