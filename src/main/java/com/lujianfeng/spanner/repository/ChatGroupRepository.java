package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.group.ChatGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatGroupRepository extends JpaRepository<ChatGroupEntity, Long> {
    Optional<ChatGroupEntity> findByGroupNo(String groupNo);

    boolean existsByGroupNo(String groupNo);
}
