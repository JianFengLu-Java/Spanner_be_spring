package com.lujianfeng.spanner.entity.emoji;

import com.lujianfeng.spanner.entity.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "emoji_favorite", uniqueConstraints = {
        @UniqueConstraint(name = "uk_emoji_favorite_emoji_user", columnNames = {"emoji_id", "user_id"})
}, indexes = {
        @Index(name = "idx_emoji_favorite_user_created", columnList = "user_id,created_at,id"),
        @Index(name = "idx_emoji_favorite_emoji", columnList = "emoji_id")
})
public class EmojiFavoriteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emoji_id", nullable = false)
    private EmojiPackEntity emoji;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
