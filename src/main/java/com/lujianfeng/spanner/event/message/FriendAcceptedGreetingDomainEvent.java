package com.lujianfeng.spanner.event.message;

public record FriendAcceptedGreetingDomainEvent(
        String fromAccount,
        String toAccount,
        String content
) {
}
