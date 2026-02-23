package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.call.CallSessionEntity;
import com.lujianfeng.spanner.entity.call.CallStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CallSessionRepository extends JpaRepository<CallSessionEntity, Long> {

    Optional<CallSessionEntity> findByCallId(String callId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CallSessionEntity c where c.callId = :callId")
    Optional<CallSessionEntity> findByCallIdForUpdate(@Param("callId") String callId);

    @Query("""
            select count(c) from CallSessionEntity c
            where (c.callerAccount = :account or c.calleeAccount = :account)
              and c.status in :activeStatuses
            """)
    long countActiveByAccount(@Param("account") String account,
                              @Param("activeStatuses") Collection<CallStatusEnum> activeStatuses);

    @Query("""
            select c from CallSessionEntity c
            where (c.callerAccount = :account or c.calleeAccount = :account)
              and c.status in :activeStatuses
            order by c.startedAt asc
            """)
    List<CallSessionEntity> findActiveByAccount(@Param("account") String account,
                                                @Param("activeStatuses") Collection<CallStatusEnum> activeStatuses);

    @Query("""
            select c from CallSessionEntity c
            where c.status = :status
              and c.expiresAt < :now
            """)
    List<CallSessionEntity> findExpiredByStatus(@Param("status") CallStatusEnum status,
                                                @Param("now") LocalDateTime now);

    Page<CallSessionEntity> findByCallerAccountOrCalleeAccountOrderByStartedAtDesc(String callerAccount,
                                                                                    String calleeAccount,
                                                                                    Pageable pageable);
}
