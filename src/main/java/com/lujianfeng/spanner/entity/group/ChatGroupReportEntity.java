package com.lujianfeng.spanner.entity.group;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "chat_group_report", indexes = {
        @Index(name = "uk_group_report_no", columnList = "report_no", unique = true),
        @Index(name = "idx_group_report_group_time", columnList = "group_id,created_at"),
        @Index(name = "idx_group_report_reporter", columnList = "reporter_account")
})
public class ChatGroupReportEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_no", nullable = false, unique = true, length = 64)
    private String reportNo;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "group_no", nullable = false, length = 32)
    private String groupNo;

    @Column(name = "reporter_account", nullable = false, length = 32)
    private String reporterAccount;

    @Column(name = "reason_type", nullable = false, length = 64)
    private String reasonType;

    @Column(length = 1000)
    private String description;

    @Column(name = "evidence_urls", length = 3000)
    private String evidenceUrls;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
