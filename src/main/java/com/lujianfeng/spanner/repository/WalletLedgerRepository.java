package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.task.WalletLedgerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletLedgerRepository extends JpaRepository<WalletLedgerEntity, String> {
    Page<WalletLedgerEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
