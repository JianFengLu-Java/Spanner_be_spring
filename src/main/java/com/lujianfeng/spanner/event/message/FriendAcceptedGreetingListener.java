package com.lujianfeng.spanner.event.message;

import com.lujianfeng.spanner.service.PrivateMessageDispatchService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class FriendAcceptedGreetingListener {

    private final PrivateMessageDispatchService privateMessageDispatchService;

    public FriendAcceptedGreetingListener(PrivateMessageDispatchService privateMessageDispatchService) {
        this.privateMessageDispatchService = privateMessageDispatchService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFriendAccepted(FriendAcceptedGreetingDomainEvent event) {
        privateMessageDispatchService.dispatchPrivateMessage(
                event.fromAccount(),
                event.toAccount(),
                event.content(),
                null,
                true
        );
    }
}
