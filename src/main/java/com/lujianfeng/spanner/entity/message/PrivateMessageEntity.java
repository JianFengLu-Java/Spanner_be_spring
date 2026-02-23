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
 * 私聊消息持久化实体
 */
@Getter
@Setter
@Entity
@Table(name = "private_message", indexes = {
        @Index(name = "idx_private_message_conversation_time", columnList = "from_account,to_account,sent_at"),
        @Index(name = "idx_private_message_to_time", columnList = "to_account,sent_at")
})
public class PrivateMessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, unique = true, length = 64)
    private String messageId;

    @Column(name = "from_account", nullable = false, length = 32)
    private String fromAccount;

    @Column(name = "to_account", nullable = false, length = 32)
    private String toAccount;

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

    @Column(name = "delivery_status", nullable = false, length = 32)
    private String deliveryStatus;

    @Column(name = "recalled", nullable = false)
    private Boolean recalled = false;

    @Column(name = "recalled_at")
    private LocalDateTime recalledAt;
}
