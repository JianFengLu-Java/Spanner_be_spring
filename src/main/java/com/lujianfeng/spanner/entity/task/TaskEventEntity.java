package com.lujianfeng.spanner.entity.task;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "task_event", indexes = {
        @Index(name = "idx_task_event_user_type_date", columnList = "actor_user_id,event_type,biz_date"),
        @Index(name = "idx_task_event_user_type_target_time", columnList = "actor_user_id,event_type,target_id,event_created_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_task_event_event_id", columnNames = "event_id")
})
public class TaskEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 32)
    private String eventType;

    @Column(name = "biz_id", nullable = false, length = 64)
    private String bizId;

    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @Column(name = "target_id", length = 64)
    private String targetId;

    @Column(name = "event_created_at", nullable = false)
    private Instant eventCreatedAt;

    @Column(name = "biz_date", nullable = false)
    private LocalDate bizDate;

    @Column(name = "meta_json", columnDefinition = "text")
    private String metaJson;

    @Column(name = "process_status", nullable = false, length = 32)
    private String processStatus;

    @Column(name = "process_reason", length = 128)
    private String processReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
