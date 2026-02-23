package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.call.CallRequestLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CallRequestLogRepository extends JpaRepository<CallRequestLogEntity, Long> {
    Optional<CallRequestLogEntity> findByRequestId(String requestId);
}
