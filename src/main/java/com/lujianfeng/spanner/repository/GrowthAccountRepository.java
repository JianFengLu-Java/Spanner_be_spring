package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.task.GrowthAccountEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GrowthAccountRepository extends JpaRepository<GrowthAccountEntity, Long> {
    Optional<GrowthAccountEntity> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GrowthAccountEntity g where g.userId = :userId")
    Optional<GrowthAccountEntity> findByUserIdForUpdate(@Param("userId") Long userId);
}
