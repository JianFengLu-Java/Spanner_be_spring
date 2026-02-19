package com.lujianfeng.spanner.entity.task;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "task_config", uniqueConstraints = {
        @UniqueConstraint(name = "uk_task_config_type", columnNames = "task_type")
})
public class TaskConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_type", nullable = false, length = 32)
    private String taskType;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "reward_wallet_cent", nullable = false)
    private Integer rewardWalletCent = 0;

    @Column(name = "reward_growth", nullable = false)
    private Integer rewardGrowth = 0;

    @Column(name = "daily_limit")
    private Integer dailyLimit;

    @Column(name = "risk_policy_json", columnDefinition = "text")
    private String riskPolicyJson;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void touch() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        } else {
            updatedAt = LocalDateTime.now();
        }
        if (enabled == null) {
            enabled = true;
        }
        if (rewardWalletCent == null) {
            rewardWalletCent = 0;
        }
        if (rewardGrowth == null) {
            rewardGrowth = 0;
        }
    }
}
