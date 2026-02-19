package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.group.ChatGroupAnnouncementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatGroupAnnouncementRepository extends JpaRepository<ChatGroupAnnouncementEntity, Long> {
    Optional<ChatGroupAnnouncementEntity> findTopByGroupIdOrderByUpdatedAtDesc(Long groupId);

    Page<ChatGroupAnnouncementEntity> findByGroupIdOrderByUpdatedAtDesc(Long groupId, Pageable pageable);

    Optional<ChatGroupAnnouncementEntity> findByAnnouncementIdAndGroupId(String announcementId, Long groupId);

    void deleteByGroupId(Long groupId);
}
