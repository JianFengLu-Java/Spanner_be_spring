package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.task.DailyCounterEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyCounterRepository extends JpaRepository<DailyCounterEntity, Long> {

    Optional<DailyCounterEntity> findByUserIdAndTaskTypeAndBizDate(Long userId, String taskType, LocalDate bizDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DailyCounterEntity d where d.userId = :userId and d.taskType = :taskType and d.bizDate = :bizDate")
    Optional<DailyCounterEntity> findByUserAndTaskAndDateForUpdate(@Param("userId") Long userId,
                                                                   @Param("taskType") String taskType,
                                                                   @Param("bizDate") LocalDate bizDate);
}
