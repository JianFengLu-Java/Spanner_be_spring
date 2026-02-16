package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.user.WalletAccountEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletAccountRepository extends JpaRepository<WalletAccountEntity, Long> {

    WalletAccountEntity findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WalletAccountEntity w where w.userId = :userId")
    WalletAccountEntity findByUserIdForUpdate(@Param("userId") Long userId);
}
