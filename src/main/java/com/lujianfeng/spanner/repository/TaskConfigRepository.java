package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.task.TaskConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaskConfigRepository extends JpaRepository<TaskConfigEntity, Long> {
    Optional<TaskConfigEntity> findByTaskType(String taskType);
}
