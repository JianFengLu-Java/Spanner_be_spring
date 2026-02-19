package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.task.TaskEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface TaskEventRepository extends JpaRepository<TaskEventEntity, Long> {
    Optional<TaskEventEntity> findByEventId(String eventId);

    @Query("select e from TaskEventEntity e where e.actorUserId = :userId and e.eventType = :eventType and e.targetId = :targetId and e.processStatus = 'GRANTED' order by e.eventCreatedAt desc")
    Optional<TaskEventEntity> findLatestGrantedByUserAndTarget(@Param("userId") Long userId,
                                                                @Param("eventType") String eventType,
                                                                @Param("targetId") String targetId);

    @Query("select count(e) from TaskEventEntity e where e.actorUserId = :userId and e.eventType = :eventType and e.targetId = :targetId and e.processStatus = 'GRANTED' and e.eventCreatedAt >= :after")
    long countGrantedSince(@Param("userId") Long userId,
                           @Param("eventType") String eventType,
                           @Param("targetId") String targetId,
                           @Param("after") Instant after);
}
