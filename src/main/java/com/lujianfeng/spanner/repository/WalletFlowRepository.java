package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.user.WalletFlowEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletFlowRepository extends JpaRepository<WalletFlowEntity, Long> {

    Page<WalletFlowEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<WalletFlowEntity> findByUserIdAndChangeTypeOrderByCreatedAtDesc(Long userId, String changeType, Pageable pageable);
}
