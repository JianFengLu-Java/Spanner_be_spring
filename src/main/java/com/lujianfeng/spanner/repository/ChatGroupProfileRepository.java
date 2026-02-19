package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.group.ChatGroupProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatGroupProfileRepository extends JpaRepository<ChatGroupProfileEntity, Long> {
    Optional<ChatGroupProfileEntity> findByGroupId(Long groupId);

    Optional<ChatGroupProfileEntity> findByGroupNo(String groupNo);

    List<ChatGroupProfileEntity> findByGroupIdIn(Collection<Long> groupIds);

    void deleteByGroupId(Long groupId);
}
