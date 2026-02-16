package com.lujianfeng.spanner.service;

import com.lujianfeng.spanner.entity.message.PrivateMessageEntity;
import com.lujianfeng.spanner.repository.PrivateMessageRepository;
import com.lujianfeng.spanner.vo.message.MessageAckVO;
import com.lujianfeng.spanner.vo.message.PrivateMessageVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 私聊消息分发服务（实时下发 + 离线兜底 + 持久化）
 */
@Service
public class PrivateMessageDispatchService {
    private static final Logger log = LoggerFactory.getLogger(PrivateMessageDispatchService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final OfflineMessageService offlineMessageService;
    private final PrivateMessageRepository privateMessageRepository;
    private final SimpUserRegistry simpUserRegistry;

    public PrivateMessageDispatchService(SimpMessagingTemplate messagingTemplate,
                                         OfflineMessageService offlineMessageService,
                                         PrivateMessageRepository privateMessageRepository,
                                         SimpUserRegistry simpUserRegistry) {
        this.messagingTemplate = messagingTemplate;
        this.offlineMessageService = offlineMessageService;
        this.privateMessageRepository = privateMessageRepository;
        this.simpUserRegistry = simpUserRegistry;
    }

    public MessageAckVO dispatchPrivateMessage(String from,
                                               String to,
                                               String content,
                                               String clientMessageId,
                                               boolean echoToSender) {
        String messageId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        PrivateMessageVO messageVO = PrivateMessageVO.builder()
                .messageId(messageId)
                .from(from)
                .to(to)
                .content(content)
                .clientMessageId(clientMessageId)
                .sentAt(now)
                .build();

        boolean receiverOnline = isUserOnline(to);
        String ackStatus;
        if (receiverOnline) {
            messagingTemplate.convertAndSendToUser(to, "/queue/messages", messageVO);
            ackStatus = "SENT";
        } else {
            boolean stored = offlineMessageService.storePrivateMessage(to, messageVO);
            ackStatus = stored ? "OFFLINE_STORED" : "OFFLINE_STORE_FAILED";
            if (!stored) {
                log.error("Failed to store private message for offline user, messageId={}, from={}, to={}",
                        messageId, from, to);
            }
        }

        persistMessage(messageVO, ackStatus);
        if (echoToSender) {
            messagingTemplate.convertAndSendToUser(from, "/queue/messages", messageVO);
        }

        return MessageAckVO.builder()
                .clientMessageId(clientMessageId)
                .messageId(messageId)
                .to(to)
                .status(ackStatus)
                .ackAt(now)
                .build();
    }

    private boolean isUserOnline(String account) {
        SimpUser user = simpUserRegistry.getUser(account);
        return user != null && !user.getSessions().isEmpty();
    }

    private void persistMessage(PrivateMessageVO messageVO, String deliveryStatus) {
        try {
            PrivateMessageEntity entity = new PrivateMessageEntity();
            entity.setMessageId(messageVO.getMessageId());
            entity.setFromAccount(messageVO.getFrom());
            entity.setToAccount(messageVO.getTo());
            entity.setContent(messageVO.getContent());
            entity.setClientMessageId(messageVO.getClientMessageId());
            entity.setSentAt(messageVO.getSentAt() == null ? LocalDateTime.now() : messageVO.getSentAt());
            entity.setDeliveryStatus(deliveryStatus == null ? "UNKNOWN" : deliveryStatus);
            privateMessageRepository.save(entity);
        } catch (Exception e) {
            log.error("Failed to persist private message, messageId={}, from={}, to={}",
                    messageVO == null ? null : messageVO.getMessageId(),
                    messageVO == null ? null : messageVO.getFrom(),
                    messageVO == null ? null : messageVO.getTo(),
                    e);
        }
    }
}
