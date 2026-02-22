package com.lujianfeng.spanner.entity.message;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "message_reaction_user",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_message_reaction_user_member",
                        columnNames = {"message_type", "message_ref_id", "reaction_key", "user_account"})
        },
        indexes = {
                @Index(name = "idx_message_reaction_user_message_key",
                        columnList = "message_type,message_ref_id,reaction_key")
        })
public class MessageReactionUserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_type", nullable = false, length = 16)
    private String messageType;

    @Column(name = "message_ref_id", nullable = false, length = 64)
    private String messageRefId;

    @Column(name = "reaction_key", nullable = false, length = 128)
    private String reactionKey;

    @Column(name = "user_account", nullable = false, length = 32)
    private String userAccount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
