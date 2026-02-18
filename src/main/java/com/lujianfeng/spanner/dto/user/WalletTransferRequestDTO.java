package com.lujianfeng.spanner.dto.user;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class WalletTransferRequestDTO {

    private String toAccount;

    private BigDecimal amount;

    private String securityPassword;

    private String businessNo;

    private String remark;
}
