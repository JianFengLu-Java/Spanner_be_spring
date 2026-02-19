package com.lujianfeng.spanner.entity.task;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "growth_account", uniqueConstraints = {
        @UniqueConstraint(name = "uk_growth_account_user", columnNames = "user_id")
})
public class GrowthAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "growth_value", nullable = false)
    private Long growthValue = 0L;

    @Column(nullable = false)
    private Integer version = 0;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void touch() {
        updatedAt = LocalDateTime.now();
        if (growthValue == null) {
            growthValue = 0L;
        }
        if (version == null) {
            version = 0;
        }
    }
}
