package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.user.UserVipOrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserVipOrderRepository extends JpaRepository<UserVipOrderEntity, Long> {

    Page<UserVipOrderEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    UserVipOrderEntity findByPurchaseNo(String purchaseNo);
}
