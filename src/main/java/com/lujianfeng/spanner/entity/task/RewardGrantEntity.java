package com.lujianfeng.spanner.entity.task;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "reward_grant", indexes = {
        @Index(name = "idx_reward_grant_user_created", columnList = "user_id,created_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_reward_grant_event", columnNames = "event_id")
})
public class RewardGrantEntity {

    @Id
    @Column(name = "grant_id", length = 64)
    private String grantId;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "task_type", nullable = false, length = 32)
    private String taskType;

    @Column(name = "wallet_cent", nullable = false)
    private Integer walletCent = 0;

    @Column(name = "growth", nullable = false)
    private Integer growth = 0;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(length = 128)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
