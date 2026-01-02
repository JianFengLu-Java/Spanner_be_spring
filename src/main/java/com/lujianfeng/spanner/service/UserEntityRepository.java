package com.lujianfeng.spanner.service;

import com.lujianfeng.spanner.entity.user.UserEntity;
import org.springframework.data.repository.Repository;

/**
 *
 *
 *
 * @author lujianfeng
 * @date 2026/1/1 10:12
 * @description
 */
interface UserEntityRepository extends Repository<UserEntity, Long> {
    UserEntity getUserEntityById(Long id);
}
