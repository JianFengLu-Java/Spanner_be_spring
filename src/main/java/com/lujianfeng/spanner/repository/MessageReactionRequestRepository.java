package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.message.MessageReactionRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageReactionRequestRepository extends JpaRepository<MessageReactionRequestEntity, Long> {
    Optional<MessageReactionRequestEntity> findByRequestIdAndOperatorAccount(String requestId, String operatorAccount);
}
