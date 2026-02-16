package com.lujianfeng.spanner.vo.user;

import com.lujianfeng.spanner.entity.user.FriendRequestDirectionEnum;
import com.lujianfeng.spanner.entity.user.UserRelationEnum;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class FriendRequestHistoryItemVO {
    private String requestId;
    private FriendRequestDirectionEnum direction;
    private UserRelationEnum status;
    private String applicantAccount;
    private String applicantName;
    private String applicantAvatarUrl;
    private String applicantSignature;
    private String targetAccount;
    private String targetName;
    private String targetAvatarUrl;
    private String targetSignature;
    private String verificationMessage;
    private String source;
    private String operatorAccount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime expiredAt;
}
