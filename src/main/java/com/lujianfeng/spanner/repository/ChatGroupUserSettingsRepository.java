package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.group.ChatGroupUserSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatGroupUserSettingsRepository extends JpaRepository<ChatGroupUserSettingsEntity, Long> {
    Optional<ChatGroupUserSettingsEntity> findByGroupIdAndUserAccount(Long groupId, String userAccount);

    void deleteByGroupId(Long groupId);
}
