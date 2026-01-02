package com.lujianfeng.spanner.service;

import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.service.service.UserRelationService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 *
 *
 *
 * @author lujianfeng
 * @date 2026/1/1 02:04
 * @description
 */
@Slf4j
@SpringBootTest
public class UserRelationServiceTest {
    @Autowired
    private UserRelationService userRelationService;
    @Autowired
    private UserEntityRepository userEntityRepository;

    @Test
    public void test() {
        UserEntity userEntity = userEntityRepository.getUserEntityById(53L);
        UserEntity friendEntity = userEntityRepository.getUserEntityById(54L);
//        userRelationService.applyRelation(userEntity, friendEntity);
        userRelationService.acceptRelation(userEntity, friendEntity);
        log.info("ok");
    }
}
