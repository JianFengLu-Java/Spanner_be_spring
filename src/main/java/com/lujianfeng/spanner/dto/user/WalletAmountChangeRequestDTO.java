package com.lujianfeng.spanner.dto.user;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class WalletAmountChangeRequestDTO {

    private BigDecimal amount;

    private String businessNo;

    private String remark;
}
