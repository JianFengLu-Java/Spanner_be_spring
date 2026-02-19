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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "chat_group_profile",
        uniqueConstraints = @UniqueConstraint(name = "uk_group_profile_group_id", columnNames = "group_id"),
        indexes = {
                @Index(name = "idx_group_profile_group_no", columnList = "group_no"),
                @Index(name = "idx_group_profile_updated_at", columnList = "updated_at")
        })
public class ChatGroupProfileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false, unique = true)
    private Long groupId;

    @Column(name = "group_no", nullable = false, length = 32)
    private String groupNo;

    @Column(name = "group_avatar_url", length = 500)
    private String groupAvatarUrl;

    @Column(length = 500)
    private String summary;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
