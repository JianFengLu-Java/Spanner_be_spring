package com.lujianfeng.spanner.vo.user;

import com.lujianfeng.spanner.entity.user.UserRelationEnum;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class FriendRequestActionResultVO {
    private String requestId;
    private UserRelationEnum status;
    private String operatorAccount;
    private OffsetDateTime updatedAt;
    private OffsetDateTime expiredAt;
}
