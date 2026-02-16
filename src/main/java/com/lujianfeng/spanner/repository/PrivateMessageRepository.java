package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.message.PrivateMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivateMessageRepository extends JpaRepository<PrivateMessageEntity, Long> {
    Page<PrivateMessageEntity> findByFromAccountAndToAccountOrFromAccountAndToAccountOrderBySentAtDesc(
            String fromA,
            String toA,
            String fromB,
            String toB,
            Pageable pageable
    );
}
