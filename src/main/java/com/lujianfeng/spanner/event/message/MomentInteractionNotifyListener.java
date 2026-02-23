package com.lujianfeng.spanner.event.message;

import com.lujianfeng.spanner.service.PrivateMessageDispatchService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MomentInteractionNotifyListener {

    private static final String SYSTEM_ACCOUNT = "SYSTEM";
    private final PrivateMessageDispatchService privateMessageDispatchService;

    public MomentInteractionNotifyListener(PrivateMessageDispatchService privateMessageDispatchService) {
        this.privateMessageDispatchService = privateMessageDispatchService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMomentInteraction(MomentInteractionNotifyDomainEvent event) {
        privateMessageDispatchService.dispatchPrivateMessage(
                SYSTEM_ACCOUNT,
                event.toAccount(),
                event.content(),
                null,
                false
        );
    }
}
