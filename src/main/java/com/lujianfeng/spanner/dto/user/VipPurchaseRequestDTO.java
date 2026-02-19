package com.lujianfeng.spanner.dto.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VipPurchaseRequestDTO {

    private String planCode;
    
    private String paymentMethod;

    private String securityPassword;

    private String purchaseNo;
}
