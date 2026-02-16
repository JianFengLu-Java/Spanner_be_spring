package com.lujianfeng.spanner.service.service;

import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.entity.user.UserRelation;
import com.lujianfeng.spanner.vo.user.FriendRequestActionResultVO;
import com.lujianfeng.spanner.vo.user.FriendRequestHistoryItemVO;
import com.lujianfeng.spanner.vo.user.PageResultVO;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * @author Lujianfeng
 * @version 1.0
 * @date 2025/12/31
 * @since 1.0
 */


public interface UserRelationService {
    FriendRequestActionResultVO applyRelation(UserEntity userEntity, UserEntity friendEntity, String verificationMessage);

    void removeRelation(UserEntity userEntity, UserEntity friendEntity);

    FriendRequestActionResultVO acceptRelation(UserEntity userEntity, UserEntity friendEntity);

    FriendRequestActionResultVO rejectRelation(UserEntity userEntity, UserEntity friendEntity);

    FriendRequestActionResultVO cancelRelation(UserEntity userEntity, String requestId);

    List<UserRelation> listFriendRelations(UserEntity userEntity);

    List<UserRelation> listPendingRequests(UserEntity userEntity);

    PageResultVO<FriendRequestHistoryItemVO> queryRequestHistory(
            UserEntity currentUser,
            int page,
            int size,
            String direction,
            Set<String> statuses,
            Instant startTime,
            Instant endTime,
            String keyword
    );

    FriendRequestHistoryItemVO queryRequestHistoryDetail(UserEntity currentUser, String requestId);

}
