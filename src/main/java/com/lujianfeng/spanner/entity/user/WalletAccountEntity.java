package com.lujianfeng.spanner.entity.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(
        name = "wallet_account",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "wallet_no"),
                @UniqueConstraint(columnNames = "user_id")
        }
)
public class WalletAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_no", nullable = false, length = 32)
    private String walletNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(name = "balance_cent")
    private Long balanceCent;

    @Column(name = "reward_version")
    private Integer rewardVersion;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "security_password", length = 100)
    private String securityPassword;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (balance == null) {
            balance = BigDecimal.ZERO.setScale(2);
        }
        if (balanceCent == null) {
            balanceCent = 0L;
        }
        if (rewardVersion == null) {
            rewardVersion = 0;
        }
        if (currency == null || currency.isBlank()) {
            currency = "CNY";
        }
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
