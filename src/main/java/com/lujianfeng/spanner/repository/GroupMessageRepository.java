package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.message.GroupMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMessageRepository extends JpaRepository<GroupMessageEntity, Long> {
    Page<GroupMessageEntity> findByGroupNoOrderBySentAtDesc(String groupNo, Pageable pageable);
}
