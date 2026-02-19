package com.lujianfeng.spanner.entity.task;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "growth_ledger", indexes = {
        @Index(name = "idx_growth_ledger_user_created", columnList = "user_id,created_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_growth_ledger_event", columnNames = "event_id")
})
public class GrowthLedgerEntity {

    @Id
    @Column(name = "ledger_id", length = 64)
    private String ledgerId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "change_growth", nullable = false)
    private Integer changeGrowth;

    @Column(name = "growth_after", nullable = false)
    private Long growthAfter;

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
