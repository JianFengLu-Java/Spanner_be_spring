package com.lujianfeng.spanner.vo.ws;

import com.lujianfeng.spanner.entity.user.UserRelationEnum;
import com.lujianfeng.spanner.event.friend.FriendRequestEventType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class FriendRequestEventVO {
    private String eventId;
    private FriendRequestEventType eventType;
    private Instant occurredAt;
    private String requestId;
    private String fromAccount;
    private String fromName;
    private String fromAvatarUrl;
    private String toAccount;
    private UserRelationEnum status;
    private long unreadPendingCount;
}
