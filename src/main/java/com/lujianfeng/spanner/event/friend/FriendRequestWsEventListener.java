package com.lujianfeng.spanner.event.friend;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class FriendRequestWsEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    public FriendRequestWsEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEvent(FriendRequestWsDomainEvent event) {
        messagingTemplate.convertAndSendToUser(event.targetAccount(), "/queue/friend-requests", event.payload());
        messagingTemplate.convertAndSendToUser(event.targetAccount(), "/queue/friend-events", event.payload());
    }
}
