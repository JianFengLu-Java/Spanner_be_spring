package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.entity.user.UserRelation;
import com.lujianfeng.spanner.entity.user.UserRelationEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author Lujianfeng
 * @version 1.0
 * @date 2025/12/31
 * @since 1.0
 */
public interface UserRelationRepository extends JpaRepository<UserRelation, Long> {
    boolean existsByUserAndFriend(UserEntity user, UserEntity friend);

    boolean existsByUserAndFriendAndRelationType(
            UserEntity user,
            UserEntity friend,
            UserRelationEnum relationType
    );

    Optional<UserRelation> findByUserAndFriend(
            UserEntity user,
            UserEntity friend
    );

    List<UserRelation> findByUserAndRelationType(
            UserEntity user,
            UserRelationEnum relationType
    );
}