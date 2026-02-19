package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.task.GrowthLedgerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrowthLedgerRepository extends JpaRepository<GrowthLedgerEntity, String> {
    Page<GrowthLedgerEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
