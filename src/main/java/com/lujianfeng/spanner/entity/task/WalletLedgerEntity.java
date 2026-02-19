package com.lujianfeng.spanner.entity.task;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "wallet_ledger", indexes = {
        @Index(name = "idx_wallet_ledger_user_created", columnList = "user_id,created_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_wallet_ledger_event", columnNames = "event_id")
})
public class WalletLedgerEntity {

    @Id
    @Column(name = "ledger_id", length = 64)
    private String ledgerId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "change_cent", nullable = false)
    private Integer changeCent;

    @Column(name = "balance_after_cent", nullable = false)
    private Long balanceAfterCent;

    @Column(name = "biz_type", nullable = false, length = 32)
    private String bizType;

    @Column(name = "task_type", nullable = false, length = 32)
    private String taskType;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "biz_id", nullable = false, length = 64)
    private String bizId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
