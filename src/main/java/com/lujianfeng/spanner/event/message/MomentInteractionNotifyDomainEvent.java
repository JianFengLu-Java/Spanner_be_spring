package com.lujianfeng.spanner.event.message;

public record MomentInteractionNotifyDomainEvent(
        String toAccount,
        String content
) {
}
