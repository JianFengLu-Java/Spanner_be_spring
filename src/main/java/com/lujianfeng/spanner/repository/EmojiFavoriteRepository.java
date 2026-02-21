package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.emoji.EmojiFavoriteEntity;
import com.lujianfeng.spanner.entity.emoji.EmojiPackEntity;
import com.lujianfeng.spanner.entity.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmojiFavoriteRepository extends JpaRepository<EmojiFavoriteEntity, Long> {
    boolean existsByEmojiAndUser(EmojiPackEntity emoji, UserEntity user);

    Optional<EmojiFavoriteEntity> findByEmojiAndUser(EmojiPackEntity emoji, UserEntity user);

    long countByEmoji(EmojiPackEntity emoji);

    long deleteByEmoji(EmojiPackEntity emoji);

    Page<EmojiFavoriteEntity> findByUserOrderByCreatedAtDesc(UserEntity user, Pageable pageable);
}
