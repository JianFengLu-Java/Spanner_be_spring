package com.lujianfeng.spanner.service.impl;

import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.entity.user.UserRelation;
import com.lujianfeng.spanner.entity.user.UserRelationEnum;
import com.lujianfeng.spanner.repository.UserRelationRepository;
import com.lujianfeng.spanner.service.service.UserRelationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lujianfeng
 * @version 1.0
 * @date 2025/12/31
 * @since 1.0
 */

@Service
public class UserRelationServiceImpl implements UserRelationService {

    private static final Logger log = LoggerFactory.getLogger(UserRelationServiceImpl.class);
    private UserRelationRepository userRelationRepository;

    @Autowired
    public void setUserRelationRepository(UserRelationRepository userRelationRepository) {
        this.userRelationRepository = userRelationRepository;
    }


    @Override
    @Transactional
    public void applyRelation(UserEntity userEntity, UserEntity friendEntity) {
        if (userEntity.getAccount().equals(friendEntity.getAccount())) {
            return;
        }
        userRelationRepository.findByUserAndFriend(userEntity, friendEntity)
                .ifPresent(relation -> {
                    switch (relation.getRelationType()) {
                        case PENDING -> throw new IllegalStateException("好友申请已存在");
                        case ACCEPTED -> throw new IllegalStateException("已经是好友");
                        case BLOCKED -> throw new IllegalStateException("你已拉黑该用户");
                    }
                });
        if (userRelationRepository.existsByUserAndFriendAndRelationType(
                userEntity,
                friendEntity,
                UserRelationEnum.BLOCKED)) {
            throw new IllegalStateException("已被对方拉黑");
        }
        UserRelation relation = new UserRelation();
        relation.setUser(userEntity);
        relation.setFriend(friendEntity);
        relation.setRelationType(UserRelationEnum.PENDING);

        userRelationRepository.save(relation);

    }

    @Override
    public void removeRelation(UserEntity userEntity, UserEntity friendEntity) {

    }

    @Override
    @Transactional
    public void acceptRelation(UserEntity userEntity, UserEntity friendEntity) {
        if (userEntity.getId().equals(friendEntity.getId())) {
            log.info("用户好友请求非法");
            return;
        }
        UserRelation relation = userRelationRepository
                .findByUserAndFriend(userEntity, friendEntity)
                .orElseThrow(() -> new IllegalStateException("好友请求不存在"));

        switch (relation.getRelationType()) {
            case ACCEPTED -> throw new IllegalStateException("好友请求已经同意");
            case BLOCKED -> throw new IllegalStateException("好友关系已被拉黑");
            case PENDING -> {
                relation.setRelationType(UserRelationEnum.ACCEPTED);
                log.info("已经同意好友请求");
            }
        }

    }
}
