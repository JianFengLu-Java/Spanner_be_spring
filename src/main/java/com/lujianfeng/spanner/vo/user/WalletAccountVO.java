package com.lujianfeng.spanner.vo.user;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class WalletAccountVO {

    private String walletNo;

    private BigDecimal balance;

    private String currency;

    private String status;

    private LocalDateTime updatedAt;
}
