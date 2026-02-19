package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.task.RewardGrantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RewardGrantRepository extends JpaRepository<RewardGrantEntity, String> {
    Optional<RewardGrantEntity> findByEventId(String eventId);
}
