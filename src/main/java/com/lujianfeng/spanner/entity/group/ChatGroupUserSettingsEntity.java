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
@Table(name = "chat_group_user_settings",
        uniqueConstraints = @UniqueConstraint(name = "uk_group_user_settings", columnNames = {"group_id", "user_account"}),
        indexes = {
                @Index(name = "idx_group_user_settings_group", columnList = "group_id"),
                @Index(name = "idx_group_user_settings_user", columnList = "user_account")
        })
public class ChatGroupUserSettingsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "group_no", nullable = false, length = 32)
    private String groupNo;

    @Column(name = "user_account", nullable = false, length = 32)
    private String userAccount;

    @Column(name = "message_mute", nullable = false)
    private Boolean messageMute;

    @Column(name = "chat_pinned", nullable = false)
    private Boolean chatPinned;

    @Column(name = "save_to_contacts", nullable = false)
    private Boolean saveToContacts;

    @Column(name = "last_cleared_at")
    private LocalDateTime lastClearedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (messageMute == null) {
            messageMute = false;
        }
        if (chatPinned == null) {
            chatPinned = false;
        }
        if (saveToContacts == null) {
            saveToContacts = false;
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
