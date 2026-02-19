package com.lujianfeng.spanner.entity.task;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "daily_counter", uniqueConstraints = {
        @UniqueConstraint(name = "uk_daily_counter_user_task_date", columnNames = {"user_id", "task_type", "biz_date"})
})
public class DailyCounterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "task_type", nullable = false, length = 32)
    private String taskType;

    @Column(name = "biz_date", nullable = false)
    private LocalDate bizDate;

    @Column(name = "granted_count", nullable = false)
    private Integer grantedCount = 0;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void touch() {
        updatedAt = LocalDateTime.now();
        if (grantedCount == null) {
            grantedCount = 0;
        }
    }
}
