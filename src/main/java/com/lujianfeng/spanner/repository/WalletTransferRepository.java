package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.user.WalletTransferEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletTransferRepository extends JpaRepository<WalletTransferEntity, Long> {

    WalletTransferEntity findByBusinessNo(String businessNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from WalletTransferEntity t where t.businessNo = :businessNo")
    WalletTransferEntity findByBusinessNoForUpdate(@Param("businessNo") String businessNo);
}
