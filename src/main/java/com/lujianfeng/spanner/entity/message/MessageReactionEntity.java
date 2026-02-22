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
@Table(name = "message_reaction",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_message_reaction_message_key",
                        columnNames = {"message_type", "message_ref_id", "reaction_key"})
        },
        indexes = {
                @Index(name = "idx_message_reaction_message_updated",
                        columnList = "message_type,message_ref_id,updated_at")
        })
public class MessageReactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_type", nullable = false, length = 16)
    private String messageType;

    @Column(name = "message_ref_id", nullable = false, length = 64)
    private String messageRefId;

    @Column(name = "reaction_key", nullable = false, length = 128)
    private String reactionKey;

    @Column(name = "emoji", length = 256)
    private String emoji;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
