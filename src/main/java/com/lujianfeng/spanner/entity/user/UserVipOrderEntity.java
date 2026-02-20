package com.lujianfeng.spanner.entity.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "user_vip_order", indexes = {
        @Index(name = "idx_user_vip_order_user_created", columnList = "user_id,created_at"),
        @Index(name = "idx_user_vip_order_purchase_no", columnList = "purchase_no", unique = true),
        @Index(name = "idx_user_vip_order_payment_order_no", columnList = "payment_order_no", unique = true)
})
public class UserVipOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "purchase_no", nullable = false, length = 64)
    private String purchaseNo;

    @Column(name = "payment_order_no", length = 64)
    private String paymentOrderNo;

    @Column(name = "payment_method", length = 32)
    private String paymentMethod;

    @Column(name = "plan_code", nullable = false, length = 16)
    private String planCode;

    @Column(name = "plan_name", nullable = false, length = 32)
    private String planName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "months", nullable = false)
    private Integer months;

    @Column(name = "growth_bonus", nullable = false)
    private Long growthBonus;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
        if (status == null || status.isBlank()) {
            status = "SUCCESS";
        }
        if (paymentMethod == null || paymentMethod.isBlank()) {
            paymentMethod = "WALLET";
        }
        if (paymentOrderNo == null || paymentOrderNo.isBlank()) {
            paymentOrderNo = purchaseNo;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
