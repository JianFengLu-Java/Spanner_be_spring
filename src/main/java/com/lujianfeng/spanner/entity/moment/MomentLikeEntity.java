package com.lujianfeng.spanner.entity.moment;

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
@Table(name = "moment_like", uniqueConstraints = {
        @UniqueConstraint(name = "uk_moment_like_moment_user", columnNames = {"moment_id", "user_id"})
}, indexes = {
        @Index(name = "idx_moment_like_moment_liked_at", columnList = "moment_id,liked_at,id"),
        @Index(name = "idx_moment_like_user", columnList = "user_id")
})
public class MomentLikeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moment_id", nullable = false)
    private MomentEntity moment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @CreationTimestamp
    @Column(name = "liked_at", nullable = false, updatable = false)
    private Instant likedAt;
}
