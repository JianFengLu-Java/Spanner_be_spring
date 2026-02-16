package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.entity.user.UserRelation;
import com.lujianfeng.spanner.entity.user.UserRelationEnum;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * @author Lujianfeng
 * @version 1.0
 * @date 2025/12/31
 * @since 1.0
 */
public interface UserRelationRepository extends JpaRepository<UserRelation, Long>, JpaSpecificationExecutor<UserRelation> {
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

    Optional<UserRelation> findByUserAndFriendAndRelationType(
            UserEntity user,
            UserEntity friend,
            UserRelationEnum relationType
    );

    Optional<UserRelation> findByRequestId(String requestId);

    long deleteByUserAndFriend(UserEntity user, UserEntity friend);

    List<UserRelation> findByUserAndRelationType(
            UserEntity user,
            UserRelationEnum relationType
    );

    List<UserRelation> findByFriendAndRelationType(
            UserEntity friend,
            UserRelationEnum relationType
    );

    long countByFriendAndRelationType(UserEntity friend, UserRelationEnum relationType);

    @Query("""
            select r from UserRelation r
            where r.relationType = :relationType
            and (r.user = :owner or r.friend = :owner)
            """)
    List<UserRelation> findAllByOwnerAndRelationType(
            @Param("owner") UserEntity owner,
            @Param("relationType") UserRelationEnum relationType
    );

    @Query("""
            select r from UserRelation r
            where (r.user = :userA and r.friend = :userB)
               or (r.user = :userB and r.friend = :userA)
            """)
    List<UserRelation> findPairRelations(
            @Param("userA") UserEntity userA,
            @Param("userB") UserEntity userB
    );
}
