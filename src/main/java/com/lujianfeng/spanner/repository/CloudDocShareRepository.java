package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.cloud.CloudDocShareEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CloudDocShareRepository extends JpaRepository<CloudDocShareEntity, Long> {

    Optional<CloudDocShareEntity> findByShareNo(String shareNo);

    Optional<CloudDocShareEntity> findByShareNoAndOwnerAccount(String shareNo, String ownerAccount);

    Page<CloudDocShareEntity> findByFriendAccountOrderByCreatedAtDesc(String friendAccount, Pageable pageable);

    List<CloudDocShareEntity> findByFriendAccountOrderByCreatedAtDesc(String friendAccount);

    List<CloudDocShareEntity> findByDocIdAndFriendAccountAndStatus(String docId, String friendAccount, String status);

    List<CloudDocShareEntity> findByDocIdAndFriendAccountAndStatusAndShareMode(String docId,
                                                                                String friendAccount,
                                                                                String status,
                                                                                String shareMode);

    boolean existsByShareNo(String shareNo);
}
