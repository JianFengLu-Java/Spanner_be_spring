package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.moment.MomentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MomentRepository extends JpaRepository<MomentEntity, String>, JpaSpecificationExecutor<MomentEntity> {
}
