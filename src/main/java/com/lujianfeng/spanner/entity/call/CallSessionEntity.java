package com.lujianfeng.spanner.entity.call;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "call_session", indexes = {
        @Index(name = "uk_call_session_call_id", columnList = "call_id", unique = true),
        @Index(name = "idx_call_session_caller_started", columnList = "caller_account,started_at"),
        @Index(name = "idx_call_session_callee_started", columnList = "callee_account,started_at"),
        @Index(name = "idx_call_session_status_expires", columnList = "status,expires_at")
})
public class CallSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "call_id", nullable = false, unique = true, length = 64)
    private String callId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CallTypeEnum type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CallStatusEnum status;

    @Column(name = "caller_account", nullable = false, length = 32)
    private String callerAccount;

    @Column(name = "caller_name", length = 64)
    private String callerName;

    @Column(name = "caller_avatar", length = 512)
    private String callerAvatar;

    @Column(name = "callee_account", nullable = false, length = 32)
    private String calleeAccount;

    @Column(name = "callee_name", length = 64)
    private String calleeName;

    @Column(name = "callee_avatar", length = 512)
    private String calleeAvatar;

    @Column(name = "channel_id", length = 80)
    private String channelId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "connected_at")
    private LocalDateTime connectedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "end_reason", length = 64)
    private String endReason;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(name = "last_heartbeat_at")
    private LocalDateTime lastHeartbeatAt;

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
        if (startedAt == null) {
            startedAt = now;
        }
        if (expiresAt == null) {
            expiresAt = now.plusSeconds(45);
        }
        if (status == null) {
            status = CallStatusEnum.RINGING;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
