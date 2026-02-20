package com.lujianfeng.spanner.entity.cloud;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "cloud_doc_share", indexes = {
        @Index(name = "uk_cloud_doc_share_no", columnList = "share_no", unique = true),
        @Index(name = "idx_cloud_doc_share_friend_status", columnList = "friend_account,status"),
        @Index(name = "idx_cloud_doc_share_owner_status", columnList = "owner_account,status"),
        @Index(name = "idx_cloud_doc_share_doc", columnList = "doc_id")
})
public class CloudDocShareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "share_no", nullable = false, unique = true, length = 64)
    private String shareNo;

    @Column(name = "doc_id", nullable = false, length = 64)
    private String docId;

    @Column(name = "owner_account", nullable = false, length = 32)
    private String ownerAccount;

    @Column(name = "friend_account", nullable = false, length = 32)
    private String friendAccount;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "share_mode", length = 16)
    private String shareMode;

    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    @Column(name = "last_viewed_at")
    private LocalDateTime lastViewedAt;

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
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
        if (shareMode == null || shareMode.isBlank()) {
            shareMode = "READONLY";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
