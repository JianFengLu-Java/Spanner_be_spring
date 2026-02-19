package com.lujianfeng.spanner.entity.user;

import java.util.Arrays;

public enum VipPaymentMethodType {
    WALLET("WALLET", "钱包支付");

    private final String code;
    private final String displayName;

    VipPaymentMethodType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static VipPaymentMethodType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return WALLET;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("paymentMethod 仅支持 WALLET"));
    }
}
