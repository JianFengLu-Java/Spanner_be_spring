package com.lujianfeng.spanner.entity.group;

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
@Table(name = "chat_group_announcement", indexes = {
        @Index(name = "uk_group_announcement_id", columnList = "announcement_id", unique = true),
        @Index(name = "idx_group_announcement_group_time", columnList = "group_id,updated_at")
})
public class ChatGroupAnnouncementEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "announcement_id", nullable = false, unique = true, length = 64)
    private String announcementId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "group_no", nullable = false, length = 32)
    private String groupNo;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "publisher_account", nullable = false, length = 32)
    private String publisherAccount;

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
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
