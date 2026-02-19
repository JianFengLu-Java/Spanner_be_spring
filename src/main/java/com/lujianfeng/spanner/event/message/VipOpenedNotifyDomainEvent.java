package com.lujianfeng.spanner.event.message;

public record VipOpenedNotifyDomainEvent(
        String toAccount,
        String content
) {
}
