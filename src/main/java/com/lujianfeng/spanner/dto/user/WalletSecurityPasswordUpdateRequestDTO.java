package com.lujianfeng.spanner.dto.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WalletSecurityPasswordUpdateRequestDTO {

    private String oldSecurityPassword;

    private String newSecurityPassword;
}
