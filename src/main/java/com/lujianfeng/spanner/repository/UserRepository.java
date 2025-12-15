package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.user.UserEntity;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    UserEntity save(@NotNull UserEntity userEntity);

    UserEntity findByUserName(String userName);
}
