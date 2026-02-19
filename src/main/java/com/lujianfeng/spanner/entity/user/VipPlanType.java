package com.lujianfeng.spanner.entity.user;

import java.math.BigDecimal;
import java.util.Arrays;

public enum VipPlanType {
    MONTHLY("MONTHLY", "月费会员", new BigDecimal("20.00"), 1, 100L),
    QUARTERLY("QUARTERLY", "季度会员", new BigDecimal("58.00"), 3, 350L),
    YEARLY("YEARLY", "年费会员", new BigDecimal("218.00"), 12, 1500L);

    private final String code;
    private final String planName;
    private final BigDecimal price;
    private final int months;
    private final long growthBonus;

    VipPlanType(String code, String planName, BigDecimal price, int months, long growthBonus) {
        this.code = code;
        this.planName = planName;
        this.price = price;
        this.months = months;
        this.growthBonus = growthBonus;
    }

    public String getCode() {
        return code;
    }

    public String getPlanName() {
        return planName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getMonths() {
        return months;
    }

    public long getGrowthBonus() {
        return growthBonus;
    }

    public static VipPlanType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return MONTHLY;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("planCode 仅支持 MONTHLY/QUARTERLY/YEARLY"));
    }
}
