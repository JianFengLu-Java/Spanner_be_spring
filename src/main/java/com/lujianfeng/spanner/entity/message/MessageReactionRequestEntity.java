package com.lujianfeng.spanner.entity.message;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "message_reaction_request",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_message_reaction_request",
                        columnNames = {"request_id", "operator_account"})
        },
        indexes = {
                @Index(name = "idx_message_reaction_request_created", columnList = "created_at")
        })
public class MessageReactionRequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, length = 128)
    private String requestId;

    @Column(name = "operator_account", nullable = false, length = 32)
    private String operatorAccount;

    @Column(name = "message_type", nullable = false, length = 16)
    private String messageType;

    @Column(name = "message_ref_id", nullable = false, length = 64)
    private String messageRefId;

    @Lob
    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
