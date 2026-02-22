package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.message.PrivateMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrivateMessageRepository extends JpaRepository<PrivateMessageEntity, Long> {
    Page<PrivateMessageEntity> findByFromAccountAndToAccountOrFromAccountAndToAccountOrderBySentAtDesc(
            String fromA,
            String toA,
            String fromB,
            String toB,
            Pageable pageable
    );

    Optional<PrivateMessageEntity> findByMessageId(String messageId);

    Optional<PrivateMessageEntity> findFirstByClientMessageIdOrderBySentAtDesc(String clientMessageId);
}
