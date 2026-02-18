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

/**
 * 群组实体
 */
@Getter
@Setter
@Entity
@Table(name = "chat_group", indexes = {
        @Index(name = "uk_chat_group_group_no", columnList = "group_no", unique = true),
        @Index(name = "idx_chat_group_owner", columnList = "owner_account")
})
public class ChatGroupEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_no", nullable = false, unique = true, length = 32)
    private String groupNo;

    @Column(name = "group_name", nullable = false, length = 64)
    private String groupName;

    @Column(name = "owner_account", nullable = false, length = 32)
    private String ownerAccount;

    @Column(length = 1000)
    private String announcement;

    @Column(name = "max_members", nullable = false)
    private Integer maxMembers;

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
        if (maxMembers == null || maxMembers < 1) {
            maxMembers = 500;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
