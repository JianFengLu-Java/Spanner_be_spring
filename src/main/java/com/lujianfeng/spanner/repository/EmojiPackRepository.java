package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.emoji.EmojiPackEntity;
import com.lujianfeng.spanner.entity.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmojiPackRepository extends JpaRepository<EmojiPackEntity, String> {
    Page<EmojiPackEntity> findByOwnerOrderByCreatedAtDesc(UserEntity owner, Pageable pageable);

    Page<EmojiPackEntity> findByOwnerAndDisplayNameContainingIgnoreCaseOrderByCreatedAtDesc(UserEntity owner,
                                                                                             String displayName,
                                                                                             Pageable pageable);

    Optional<EmojiPackEntity> findByIdAndOwner(String id, UserEntity owner);
}
