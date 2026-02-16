package com.lujianfeng.spanner.entity.user;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * @author Lujianfeng
 * @version 1.0
 * @date 2025/12/31
 * @since 1.0
 */

@Getter
@Setter
@ToString(exclude = {"user", "friend"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(
        name = "user_relation",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "friend_id"}),
                @UniqueConstraint(columnNames = {"request_id"})
        },
        indexes = {
                @Index(name = "idx_relation_user_status_updated", columnList = "user_id,relation_type,updated_at"),
                @Index(name = "idx_relation_user_created", columnList = "user_id,create_time"),
                @Index(name = "idx_relation_user_friend", columnList = "user_id,friend_id")
        }
)
public class UserRelation {
    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
    @JoinColumn(name = "friend_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private UserEntity friend;
    @Column(length = 64, name = "request_id")
    private String requestId;
    @Enumerated(EnumType.STRING)
    private UserRelationEnum relationType;
    @Column(length = 255)
    private String verificationMessage;
    @Column(length = 64)
    private String source;
    @Column(length = 64)
    private String operatorAccount;
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @Column(name = "expired_at")
    private Instant expiredAt;
    @Version
    private Long version;
}
