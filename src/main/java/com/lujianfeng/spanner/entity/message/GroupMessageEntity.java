package com.lujianfeng.spanner.entity.message;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 群消息实体
 */
@Getter
@Setter
@Entity
@Table(name = "group_message", indexes = {
        @Index(name = "idx_group_message_group_no_time", columnList = "group_no,sent_at"),
        @Index(name = "idx_group_message_from_time", columnList = "from_account,sent_at")
})
public class GroupMessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, unique = true, length = 64)
    private String messageId;

    @Column(name = "group_no", nullable = false, length = 32)
    private String groupNo;

    @Column(name = "from_account", nullable = false, length = 32)
    private String fromAccount;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "quoted_message_id", length = 64)
    private String quotedMessageId;

    @Column(name = "quoted_from_account", length = 32)
    private String quotedFromAccount;

    @Column(name = "quoted_content", length = 2000)
    private String quotedContent;

    @Column(name = "client_message_id", length = 128)
    private String clientMessageId;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "recalled", nullable = false)
    private Boolean recalled = false;

    @Column(name = "recalled_at")
    private LocalDateTime recalledAt;
}
