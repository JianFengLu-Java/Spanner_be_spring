package com.lujianfeng.spanner.service;

import com.lujianfeng.spanner.entity.message.PrivateMessageEntity;
import com.lujianfeng.spanner.repository.PrivateMessageRepository;
import com.lujianfeng.spanner.repository.UserRepository;
import com.lujianfeng.spanner.vo.message.MessageAckVO;
import com.lujianfeng.spanner.vo.message.MessageQuoteVO;
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
    private static final String SYSTEM_ACCOUNT = "SYSTEM";

    private final SimpMessagingTemplate messagingTemplate;
    private final OfflineMessageService offlineMessageService;
    private final PrivateMessageRepository privateMessageRepository;
    private final SimpUserRegistry simpUserRegistry;
    private final UserRepository userRepository;

    public PrivateMessageDispatchService(SimpMessagingTemplate messagingTemplate,
                                         OfflineMessageService offlineMessageService,
                                         PrivateMessageRepository privateMessageRepository,
                                         SimpUserRegistry simpUserRegistry,
                                         UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.offlineMessageService = offlineMessageService;
        this.privateMessageRepository = privateMessageRepository;
        this.simpUserRegistry = simpUserRegistry;
        this.userRepository = userRepository;
    }

    public MessageAckVO dispatchPrivateMessage(String from,
                                               String to,
                                               String content,
                                               String clientMessageId,
                                               boolean echoToSender) {
        return dispatchPrivateMessage(from, to, content, null, clientMessageId, echoToSender);
    }

    public MessageAckVO dispatchPrivateMessage(String from,
                                               String to,
                                               String content,
                                               MessageQuoteVO quote,
                                               String clientMessageId,
                                               boolean echoToSender) {
        String messageId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        UserProfile fromProfile = resolveUserProfile(from);
        MessageQuoteVO normalizedQuote = enrichQuote(quote);
        PrivateMessageVO messageVO = PrivateMessageVO.builder()
                .messageId(messageId)
                .from(from)
                .formName(fromProfile.realName())
                .fromName(fromProfile.realName())
                .fromRealName(fromProfile.realName())
                .fromAvatarUrl(fromProfile.avatarUrl())
                .to(to)
                .content(content)
                .quote(normalizedQuote)
                .clientMessageId(clientMessageId)
                .sentAt(now)
                .recalled(false)
                .recalledAt(null)
                .build();

        boolean receiverOnline = isUserOnline(to);
        String ackStatus;
        if (isSystemSender(from)) {
            // 系统通知优先尝试实时投递，避免在线态误判导致前端收不到即时通知。
            messagingTemplate.convertAndSendToUser(to, "/queue/messages", messageVO);
            if (receiverOnline) {
                ackStatus = "SENT";
            } else {
                boolean stored = offlineMessageService.storePrivateMessage(to, messageVO);
                ackStatus = stored ? "OFFLINE_STORED" : "OFFLINE_STORE_FAILED";
                if (!stored) {
                    log.error("Failed to store private message for offline user, messageId={}, from={}, to={}",
                            messageId, from, to);
                }
            }
        } else if (receiverOnline) {
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
            entity.setQuotedMessageId(messageVO.getQuote() == null ? null : messageVO.getQuote().getMessageId());
            entity.setQuotedFromAccount(messageVO.getQuote() == null ? null : messageVO.getQuote().getFrom());
            entity.setQuotedContent(messageVO.getQuote() == null ? null : messageVO.getQuote().getContent());
            entity.setClientMessageId(messageVO.getClientMessageId());
            entity.setSentAt(messageVO.getSentAt() == null ? LocalDateTime.now() : messageVO.getSentAt());
            entity.setDeliveryStatus(deliveryStatus == null ? "UNKNOWN" : deliveryStatus);
            entity.setRecalled(Boolean.TRUE.equals(messageVO.getRecalled()));
            entity.setRecalledAt(messageVO.getRecalledAt());
            privateMessageRepository.save(entity);
        } catch (Exception e) {
            log.error("Failed to persist private message, messageId={}, from={}, to={}",
                    messageVO == null ? null : messageVO.getMessageId(),
                    messageVO == null ? null : messageVO.getFrom(),
                    messageVO == null ? null : messageVO.getTo(),
                    e);
        }
    }

    private MessageQuoteVO enrichQuote(MessageQuoteVO quote) {
        if (quote == null) {
            return null;
        }
        String quoteFromRealName = quote.getFromRealName();
        String quoteFromAvatarUrl = quote.getFromAvatarUrl();
        if (quote.getFrom() != null && !quote.getFrom().isBlank()
                && ((quoteFromRealName == null || quoteFromRealName.isBlank())
                || (quoteFromAvatarUrl == null || quoteFromAvatarUrl.isBlank()))) {
            UserProfile quoteProfile = resolveUserProfile(quote.getFrom());
            if (quoteFromRealName == null || quoteFromRealName.isBlank()) {
                quoteFromRealName = quoteProfile.realName();
            }
            if (quoteFromAvatarUrl == null || quoteFromAvatarUrl.isBlank()) {
                quoteFromAvatarUrl = quoteProfile.avatarUrl();
            }
        }
        return MessageQuoteVO.builder()
                .messageId(quote.getMessageId())
                .from(quote.getFrom())
                .formName(quoteFromRealName)
                .fromName(quoteFromRealName)
                .fromRealName(quoteFromRealName)
                .fromAvatarUrl(quoteFromAvatarUrl)
                .content(quote.getContent())
                .build();
    }

    private UserProfile resolveUserProfile(String account) {
        if (account == null || account.isBlank()) {
            return new UserProfile(null, null);
        }
        var user = userRepository.findByAccount(account);
        if (user == null) {
            return new UserProfile(account, null);
        }
        String realName = user.getRealName();
        if (realName == null || realName.isBlank()) {
            realName = account;
        }
        return new UserProfile(realName, user.getAvatarUrl());
    }

    private record UserProfile(String realName, String avatarUrl) {
    }

    private boolean isSystemSender(String account) {
        return account != null && SYSTEM_ACCOUNT.equalsIgnoreCase(account.trim());
    }
}
