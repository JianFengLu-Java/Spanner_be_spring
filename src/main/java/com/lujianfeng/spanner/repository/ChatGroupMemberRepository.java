package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.group.ChatGroupMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatGroupMemberRepository extends JpaRepository<ChatGroupMemberEntity, Long> {
    Optional<ChatGroupMemberEntity> findByGroupIdAndUserAccount(Long groupId, String userAccount);

    List<ChatGroupMemberEntity> findByGroupIdOrderByJoinedAtAsc(Long groupId);

    long countByGroupId(Long groupId);

    boolean existsByGroupIdAndUserAccount(Long groupId, String userAccount);

    void deleteByGroupIdAndUserAccount(Long groupId, String userAccount);

    void deleteByGroupId(Long groupId);
}
