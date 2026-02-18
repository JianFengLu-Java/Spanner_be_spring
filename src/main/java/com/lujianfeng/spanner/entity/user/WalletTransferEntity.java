package com.lujianfeng.spanner.entity.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "wallet_transfer", indexes = {
        @Index(name = "idx_wallet_transfer_from_created", columnList = "from_user_id,created_at"),
        @Index(name = "idx_wallet_transfer_to_created", columnList = "to_user_id,created_at")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = "business_no")
})
public class WalletTransferEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_no", nullable = false, length = 64)
    private String businessNo;

    @Column(name = "from_user_id", nullable = false)
    private Long fromUserId;

    @Column(name = "to_user_id", nullable = false)
    private Long toUserId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String remark;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

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
        if (status == null || status.isBlank()) {
            status = "PENDING";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
