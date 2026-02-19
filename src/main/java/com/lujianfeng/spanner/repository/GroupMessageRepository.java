package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.message.GroupMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GroupMessageRepository extends JpaRepository<GroupMessageEntity, Long> {
    Page<GroupMessageEntity> findByGroupNoOrderBySentAtDesc(String groupNo, Pageable pageable);

    @Query(value = "select count(1) from group_message where group_no = :groupNo", nativeQuery = true)
    long countByGroupNo(String groupNo);

    @Query(value = "select count(1) from group_message where group_no = :groupNo and content ~* '(https?://)'", nativeQuery = true)
    long countLinkMessagesByGroupNo(String groupNo);

    @Query(value = "select count(1) from group_message where group_no = :groupNo and content ~* '\\\\.(png|jpg|jpeg|gif|webp|mp4|mov|avi|mkv)(\\\\?|$)'", nativeQuery = true)
    long countImageOrVideoMessagesByGroupNo(String groupNo);

    @Query(value = "select count(1) from group_message where group_no = :groupNo and content ~* '\\\\.(pdf|doc|docx|xls|xlsx|ppt|pptx|zip|rar|txt)(\\\\?|$)'", nativeQuery = true)
    long countFileMessagesByGroupNo(String groupNo);

    void deleteByGroupNo(String groupNo);
}
