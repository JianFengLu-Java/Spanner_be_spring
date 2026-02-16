package com.lujianfeng.spanner.event.friend;

import com.lujianfeng.spanner.vo.ws.FriendRequestEventVO;

public record FriendRequestWsDomainEvent(
        String targetAccount,
        FriendRequestEventVO payload
) {
}
