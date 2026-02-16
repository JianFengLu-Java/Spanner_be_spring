package com.lujianfeng.spanner.entity.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "wallet_flow", indexes = {
        @Index(name = "idx_wallet_flow_user_created", columnList = "user_id,created_at"),
        @Index(name = "idx_wallet_flow_business_no", columnList = "business_no")
})
public class WalletFlowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(name = "wallet_no", nullable = false, length = 32)
    private String walletNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "business_no", nullable = false, length = 64)
    private String businessNo;

    @Column(name = "change_type", nullable = false, length = 16)
    private String changeType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "before_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal beforeBalance;

    @Column(name = "after_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal afterBalance;

    @Column(length = 255)
    private String remark;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
