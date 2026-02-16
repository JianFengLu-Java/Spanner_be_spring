package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.moment.MomentCommentEntity;
import com.lujianfeng.spanner.entity.moment.MomentEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MomentCommentRepository extends JpaRepository<MomentCommentEntity, String> {
    long deleteByMoment(MomentEntity moment);

    Optional<MomentCommentEntity> findByIdAndMoment(String id, MomentEntity moment);

    @Query("""
            select c from MomentCommentEntity c
            where c.moment = :moment
              and (
                (:parentCommentId is null and c.parentCommentId is null)
                or (:parentCommentId is not null and c.parentCommentId = :parentCommentId)
              )
            order by c.createdAt desc, c.id desc
            """)
    List<MomentCommentEntity> findLatestFirstPage(@Param("moment") MomentEntity moment,
                                                  @Param("parentCommentId") String parentCommentId,
                                                  Pageable pageable);

    @Query("""
            select c from MomentCommentEntity c
            where c.moment = :moment
              and (
                (:parentCommentId is null and c.parentCommentId is null)
                or (:parentCommentId is not null and c.parentCommentId = :parentCommentId)
              )
              and (
                c.createdAt < :cursorTime
                or (c.createdAt = :cursorTime and c.id < :cursorId)
              )
            order by c.createdAt desc, c.id desc
            """)
    List<MomentCommentEntity> findLatestPage(@Param("moment") MomentEntity moment,
                                             @Param("parentCommentId") String parentCommentId,
                                             @Param("cursorTime") Instant cursorTime,
                                             @Param("cursorId") String cursorId,
                                             Pageable pageable);

    @Query("""
            select c from MomentCommentEntity c
            where c.moment = :moment
              and (
                (:parentCommentId is null and c.parentCommentId is null)
                or (:parentCommentId is not null and c.parentCommentId = :parentCommentId)
              )
            order by c.likesCount desc, c.createdAt desc, c.id desc
            """)
    List<MomentCommentEntity> findHotFirstPage(@Param("moment") MomentEntity moment,
                                               @Param("parentCommentId") String parentCommentId,
                                               Pageable pageable);

    @Query("""
            select c from MomentCommentEntity c
            where c.moment = :moment
              and (
                (:parentCommentId is null and c.parentCommentId is null)
                or (:parentCommentId is not null and c.parentCommentId = :parentCommentId)
              )
              and (
                c.createdAt < :cursorTime
                or (c.createdAt = :cursorTime and c.id < :cursorId)
              )
            order by c.likesCount desc, c.createdAt desc, c.id desc
            """)
    List<MomentCommentEntity> findHotPage(@Param("moment") MomentEntity moment,
                                          @Param("parentCommentId") String parentCommentId,
                                          @Param("cursorTime") Instant cursorTime,
                                          @Param("cursorId") String cursorId,
                                          Pageable pageable);

    long countByMomentAndParentCommentId(MomentEntity moment, String parentCommentId);
}
