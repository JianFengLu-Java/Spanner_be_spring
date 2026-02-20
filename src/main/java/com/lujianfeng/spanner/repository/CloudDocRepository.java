package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.cloud.CloudDocEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CloudDocRepository extends JpaRepository<CloudDocEntity, Long> {

    Optional<CloudDocEntity> findByDocIdAndDeletedFalse(String docId);

    Optional<CloudDocEntity> findByDocIdAndOwnerAccountAndDeletedFalse(String docId, String ownerAccount);

    Page<CloudDocEntity> findByOwnerAccountAndDeletedFalse(String ownerAccount, Pageable pageable);

    Page<CloudDocEntity> findByOwnerAccountAndDeletedFalseAndTitleContainingIgnoreCase(String ownerAccount,
                                                                                         String keyword,
                                                                                         Pageable pageable);

    boolean existsByDocId(String docId);
}
