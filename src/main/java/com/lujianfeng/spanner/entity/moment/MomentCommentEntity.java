package com.lujianfeng.spanner.entity.moment;

import com.lujianfeng.spanner.entity.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "moment_comment", indexes = {
        @Index(name = "idx_moment_comment_moment_parent_created", columnList = "moment_id,parent_comment_id,created_at,id"),
        @Index(name = "idx_moment_comment_parent", columnList = "parent_comment_id")
})
public class MomentCommentEntity {
    @Id
    @Column(length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moment_id", nullable = false)
    private MomentEntity moment;

    @Column(name = "parent_comment_id", length = 64)
    private String parentCommentId;

    @Column(name = "reply_to_account", length = 32)
    private String replyToAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private UserEntity author;

    @Column(nullable = false, length = 500)
    private String text;

    @Column(name = "likes_count", nullable = false)
    private Long likesCount = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
