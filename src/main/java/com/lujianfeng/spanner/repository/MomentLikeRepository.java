package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.moment.MomentEntity;
import com.lujianfeng.spanner.entity.moment.MomentLikeEntity;
import com.lujianfeng.spanner.entity.user.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MomentLikeRepository extends JpaRepository<MomentLikeEntity, Long> {
    long deleteByMoment(MomentEntity moment);

    boolean existsByMomentAndUser(MomentEntity moment, UserEntity user);

    Optional<MomentLikeEntity> findByMomentAndUser(MomentEntity moment, UserEntity user);

    List<MomentLikeEntity> findTop3ByMomentOrderByLikedAtDescIdDesc(MomentEntity moment);

    @Query("""
            select l.moment.id from MomentLikeEntity l
            where l.user = :user and l.moment.id in :momentIds
            """)
    List<String> findLikedMomentIdsByUserAndMomentIds(@Param("user") UserEntity user, @Param("momentIds") List<String> momentIds);

    @Query("""
            select l from MomentLikeEntity l
            where l.moment = :moment
              and (
                :cursorTime is null
                or l.likedAt < :cursorTime
                or (l.likedAt = :cursorTime and l.id < :cursorId)
              )
            order by l.likedAt desc, l.id desc
            """)
    List<MomentLikeEntity> findLikePage(@Param("moment") MomentEntity moment,
                                        @Param("cursorTime") Instant cursorTime,
                                        @Param("cursorId") Long cursorId,
                                        Pageable pageable);
}
