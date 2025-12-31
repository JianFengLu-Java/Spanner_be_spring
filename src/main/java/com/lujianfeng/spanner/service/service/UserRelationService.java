package com.lujianfeng.spanner.service.service;

import com.lujianfeng.spanner.entity.user.UserEntity;

/**
 * @author Lujianfeng
 * @version 1.0
 * @date 2025/12/31
 * @since 1.0
 */


public interface UserRelationService {
    void applyRelation(UserEntity userEntity, UserEntity friendEntity);

    void removeRelation(UserEntity userEntity, UserEntity friendEntity);

    void acceptRelation(UserEntity userEntity, UserEntity friendEntity);

}
