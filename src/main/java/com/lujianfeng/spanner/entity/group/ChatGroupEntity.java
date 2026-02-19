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

    @Column(name = "group_avatar_url", length = 500)
    private String groupAvatarUrl;

    @Column(length = 500)
    private String summary;

    @Column(name = "invite_mode", length = 32)
    private String inviteMode;

    @Column(name = "member_can_edit_group_name")
    private Boolean memberCanEditGroupName;

    @Column(name = "join_verification_enabled")
    private Boolean joinVerificationEnabled;

    @Column(name = "announcement_permission", length = 32)
    private String announcementPermission;

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
        if (inviteMode == null || inviteMode.isBlank()) {
            inviteMode = GroupInviteModeEnum.ADMIN_ONLY.name();
        }
        if (memberCanEditGroupName == null) {
            memberCanEditGroupName = false;
        }
        if (joinVerificationEnabled == null) {
            joinVerificationEnabled = true;
        }
        if (announcementPermission == null || announcementPermission.isBlank()) {
            announcementPermission = GroupAnnouncementPermissionEnum.OWNER_ADMIN.name();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
