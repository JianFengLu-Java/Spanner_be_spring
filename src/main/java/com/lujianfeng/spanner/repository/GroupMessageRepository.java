package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.message.GroupMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GroupMessageRepository extends JpaRepository<GroupMessageEntity, Long> {
    Page<GroupMessageEntity> findByGroupNoOrderBySentAtDesc(String groupNo, Pageable pageable);

    @Query(value = "select count(1) from group_message where group_no = :groupNo", nativeQuery = true)
    long countByGroupNo(@Param("groupNo") String groupNo);

    @Query(value = "select count(1) from group_message where group_no = :groupNo and content ~* '(https?://)'", nativeQuery = true)
    long countLinkMessagesByGroupNo(@Param("groupNo") String groupNo);

    @Query(value = "select count(1) from group_message where group_no = :groupNo and content ~* '\\\\.(png|jpg|jpeg|gif|webp|mp4|mov|avi|mkv)(\\\\?|$)'", nativeQuery = true)
    long countImageOrVideoMessagesByGroupNo(@Param("groupNo") String groupNo);

    @Query(value = "select count(1) from group_message where group_no = :groupNo and content ~* '\\\\.(pdf|doc|docx|xls|xlsx|ppt|pptx|zip|rar|txt)(\\\\?|$)'", nativeQuery = true)
    long countFileMessagesByGroupNo(@Param("groupNo") String groupNo);

    void deleteByGroupNo(String groupNo);

    Optional<GroupMessageEntity> findByMessageId(String messageId);

    Optional<GroupMessageEntity> findFirstByClientMessageIdOrderBySentAtDesc(String clientMessageId);
}
