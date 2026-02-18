package com.lujianfeng.spanner.entity.group;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 群成员实体
 */
@Getter
@Setter
@Entity
@Table(name = "chat_group_member",
        uniqueConstraints = @UniqueConstraint(name = "uk_group_member_group_user", columnNames = {"group_id", "user_account"}),
        indexes = {
                @Index(name = "idx_group_member_group_id", columnList = "group_id"),
                @Index(name = "idx_group_member_user_account", columnList = "user_account"),
                @Index(name = "idx_group_member_role", columnList = "role")
        })
public class ChatGroupMemberEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "user_account", nullable = false, length = 32)
    private String userAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GroupMemberRoleEnum role;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    public void prePersist() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }
}
