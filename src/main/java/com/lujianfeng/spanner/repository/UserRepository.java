package com.lujianfeng.spanner.repository;

import com.lujianfeng.spanner.entity.user.UserEntity;
import jakarta.persistence.LockModeType;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * @author Lujianfeng
 * @version 1.0
 * @date 2026/1/8
 * @since 1.0
 */
@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    UserEntity save(@NotNull UserEntity userEntity);
    

    @Query(value = "SELECT nextval('user_account_seq')", nativeQuery = true)
    Long nextAccount();

    UserEntity findByAccount(String account);

    List<UserEntity> findByAccountIn(Collection<String> accounts);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.id = :id")
    UserEntity findByIdForUpdate(@Param("id") Long id);
}
