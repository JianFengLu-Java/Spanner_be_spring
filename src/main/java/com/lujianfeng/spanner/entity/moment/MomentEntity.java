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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "moment", indexes = {
        @Index(name = "idx_moment_created_at", columnList = "created_at,id"),
        @Index(name = "idx_moment_author_created", columnList = "author_id,created_at")
})
public class MomentEntity {
    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(length = 1024)
    private String cover;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private UserEntity author;

    @Column(name = "content_text", columnDefinition = "text")
    private String contentText;

    @Column(name = "content_html", columnDefinition = "text")
    private String contentHtml;

    @Column(name = "images_json", columnDefinition = "text")
    private String imagesJson;

    @Column(name = "likes_count", nullable = false)
    private Long likesCount = 0L;

    @Column(name = "comments_count", nullable = false)
    private Long commentsCount = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
