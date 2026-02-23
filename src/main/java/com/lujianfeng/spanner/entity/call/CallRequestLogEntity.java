package com.lujianfeng.spanner.entity.call;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "call_request_log", indexes = {
        @Index(name = "uk_call_request_log_request_id", columnList = "request_id", unique = true),
        @Index(name = "idx_call_request_log_call_id", columnList = "call_id")
})
public class CallRequestLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true, length = 128)
    private String requestId;

    @Column(name = "call_id", length = 64)
    private String callId;

    @Column(name = "actor_account", nullable = false, length = 32)
    private String actorAccount;

    @Column(name = "action", nullable = false, length = 32)
    private String action;

    @Column(name = "result_status", length = 32)
    private String resultStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
