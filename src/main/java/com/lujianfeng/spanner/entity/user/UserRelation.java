package com.lujianfeng.spanner.entity.user;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

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
@Table(name = "user_relation", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "friend_id"})
})
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
    @Enumerated(EnumType.STRING)
    private UserRelationEnum relationType;
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;
}


