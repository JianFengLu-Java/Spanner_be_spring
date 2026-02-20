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
@Table(name = "cloud_doc", indexes = {
        @Index(name = "uk_cloud_doc_doc_id", columnList = "doc_id", unique = true),
        @Index(name = "idx_cloud_doc_owner_updated", columnList = "owner_account,updated_at"),
        @Index(name = "idx_cloud_doc_owner_deleted", columnList = "owner_account,deleted")
})
public class CloudDocEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doc_id", nullable = false, unique = true, length = 64)
    private String docId;

    @Column(name = "owner_account", nullable = false, length = 32)
    private String ownerAccount;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(length = 280)
    private String snippet;

    @Column(name = "content_html", columnDefinition = "TEXT")
    private String contentHtml;

    @Column(name = "content_json", columnDefinition = "TEXT")
    private String contentJson;

    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private Boolean deleted;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_saved_at", nullable = false)
    private LocalDateTime lastSavedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (lastSavedAt == null) {
            lastSavedAt = now;
        }
        if (version == null || version < 1) {
            version = 1L;
        }
        if (deleted == null) {
            deleted = false;
        }
        if (title == null || title.isBlank()) {
            title = "未标题云文档";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
