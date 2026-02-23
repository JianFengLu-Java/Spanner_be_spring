package com.lujianfeng.spanner.service;

import com.lujianfeng.spanner.dto.message.MessageReactionToggleRequestDTO;
import com.lujianfeng.spanner.dto.message.ReactionDTO;
import com.lujianfeng.spanner.entity.group.ChatGroupEntity;
import com.lujianfeng.spanner.entity.group.ChatGroupMemberEntity;
import com.lujianfeng.spanner.entity.message.GroupMessageEntity;
import com.lujianfeng.spanner.entity.message.MessageReactionEntity;
import com.lujianfeng.spanner.entity.message.MessageReactionRequestEntity;
import com.lujianfeng.spanner.entity.message.MessageReactionUserEntity;
import com.lujianfeng.spanner.entity.message.PrivateMessageEntity;
import com.lujianfeng.spanner.repository.ChatGroupMemberRepository;
import com.lujianfeng.spanner.repository.GroupMessageRepository;
import com.lujianfeng.spanner.repository.MessageReactionRepository;
import com.lujianfeng.spanner.repository.MessageReactionRequestRepository;
import com.lujianfeng.spanner.repository.MessageReactionUserRepository;
import com.lujianfeng.spanner.repository.PrivateMessageRepository;
import com.lujianfeng.spanner.repository.UserRepository;
import com.lujianfeng.spanner.vo.message.MessageReactionSnapshotVO;
import com.lujianfeng.spanner.vo.message.MessageReactionUpdatedEventVO;
import com.lujianfeng.spanner.vo.message.ReactionItemVO;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class MessageReactionService {
    private static final Logger log = LoggerFactory.getLogger(MessageReactionService.class);
    private static final String TYPE_PRIVATE = "PRIVATE";
    private static final String TYPE_GROUP = "GROUP";
    private static final String EVENT_TYPE = "message.reactions.updated";
    private static final String EVENT_DESTINATION = "/queue/message.reactions.updated";

    private final PrivateMessageRepository privateMessageRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final ChatGroupService chatGroupService;
    private final ChatGroupMemberRepository chatGroupMemberRepository;
    private final MessageReactionRepository messageReactionRepository;
    private final MessageReactionUserRepository messageReactionUserRepository;
    private final MessageReactionRequestRepository messageReactionRequestRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final PrivateMessageDispatchService privateMessageDispatchService;
    private final UserRepository userRepository;

    public MessageReactionService(PrivateMessageRepository privateMessageRepository,
                                  GroupMessageRepository groupMessageRepository,
                                  ChatGroupService chatGroupService,
                                  ChatGroupMemberRepository chatGroupMemberRepository,
                                  MessageReactionRepository messageReactionRepository,
                                  MessageReactionUserRepository messageReactionUserRepository,
                                  MessageReactionRequestRepository messageReactionRequestRepository,
                                  SimpMessagingTemplate messagingTemplate,
                                  PrivateMessageDispatchService privateMessageDispatchService,
                                  UserRepository userRepository) {
        this.privateMessageRepository = privateMessageRepository;
        this.groupMessageRepository = groupMessageRepository;
        this.chatGroupService = chatGroupService;
        this.chatGroupMemberRepository = chatGroupMemberRepository;
        this.messageReactionRepository = messageReactionRepository;
        this.messageReactionUserRepository = messageReactionUserRepository;
        this.messageReactionRequestRepository = messageReactionRequestRepository;
        this.messagingTemplate = messagingTemplate;
        this.privateMessageDispatchService = privateMessageDispatchService;
        this.userRepository = userRepository;
    }

    @Transactional
    public MessageReactionSnapshotVO toggleReaction(String operatorAccount,
                                                    String pathMessageId,
                                                    MessageReactionToggleRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (request.getChatId() == null) {
            throw new IllegalArgumentException("chatId 不能为空");
        }
        String requestId = trim(request.getRequestId());
        if (requestId == null) {
            throw new IllegalArgumentException("requestId 不能为空");
        }

        ReactionDTO reactionInput = request.getReaction();
        String reactionKey = trim(reactionInput == null ? null : reactionInput.getKey());
        String emoji = trim(reactionInput == null ? null : reactionInput.getEmoji());
        String imageUrl = trim(reactionInput == null ? null : reactionInput.getImageUrl());
        if (reactionKey == null) {
            throw new IllegalArgumentException("reaction.key 不能为空");
        }
        if (emoji == null && imageUrl == null) {
            throw new IllegalArgumentException("emoji 与 imageUrl 不能同时为空");
        }

        MessageTarget target = resolveMessage(pathMessageId, request.getServerMessageId(), request.getClientMessageId());
        ensurePermission(target, operatorAccount);

        MessageReactionRequestEntity existingRequest =
                messageReactionRequestRepository.findByRequestIdAndOperatorAccount(requestId, operatorAccount).orElse(null);
        if (existingRequest != null) {
            return buildSnapshot(target, request.getChatId());
        }

        MessageReactionEntity reactionEntity =
                messageReactionRepository.findByMessageTypeAndMessageRefIdAndReactionKey(target.messageType(),
                        target.messageRefId(), reactionKey).orElseGet(() -> {
                    MessageReactionEntity created = new MessageReactionEntity();
                    created.setMessageType(target.messageType());
                    created.setMessageRefId(target.messageRefId());
                    created.setReactionKey(reactionKey);
                    created.setUpdatedAt(Instant.now());
                    return created;
                });

        MessageReactionUserEntity relation =
                messageReactionUserRepository.findByMessageTypeAndMessageRefIdAndReactionKeyAndUserAccount(
                        target.messageType(), target.messageRefId(), reactionKey, operatorAccount).orElse(null);
        boolean toggledOn;
        if (relation == null) {
            MessageReactionUserEntity created = new MessageReactionUserEntity();
            created.setMessageType(target.messageType());
            created.setMessageRefId(target.messageRefId());
            created.setReactionKey(reactionKey);
            created.setUserAccount(operatorAccount);
            created.setCreatedAt(Instant.now());
            messageReactionUserRepository.save(created);
            toggledOn = true;
        } else {
            messageReactionUserRepository.delete(relation);
            toggledOn = false;
        }

        long memberCount = messageReactionUserRepository.countByMessageTypeAndMessageRefIdAndReactionKey(
                target.messageType(), target.messageRefId(), reactionKey);
        if (memberCount <= 0) {
            messageReactionRepository.deleteByMessageTypeAndMessageRefIdAndReactionKey(
                    target.messageType(), target.messageRefId(), reactionKey);
        } else {
            reactionEntity.setEmoji(emoji);
            reactionEntity.setImageUrl(imageUrl);
            reactionEntity.setUpdatedAt(Instant.now());
            messageReactionRepository.save(reactionEntity);
        }

        MessageReactionSnapshotVO snapshot = buildSnapshot(target, request.getChatId());
        persistRequestSnapshot(requestId, operatorAccount, target);
        publishUpdatedEvent(target, snapshot);
        maybeSendReactionSystemNotify(target, operatorAccount, toggledOn, emoji, imageUrl, reactionKey);
        return snapshot;
    }

    public MessageReactionSnapshotVO getSnapshot(String operatorAccount,
                                                 String pathMessageId,
                                                 Long chatId,
                                                 String serverMessageId,
                                                 String clientMessageId) {
        if (chatId == null) {
            throw new IllegalArgumentException("chatId 不能为空");
        }
        MessageTarget target = resolveMessage(pathMessageId, serverMessageId, clientMessageId);
        ensurePermission(target, operatorAccount);
        return buildSnapshot(target, chatId);
    }

    private void persistRequestSnapshot(String requestId,
                                        String operatorAccount,
                                        MessageTarget target) {
        try {
            MessageReactionRequestEntity entity = new MessageReactionRequestEntity();
            entity.setRequestId(requestId);
            entity.setOperatorAccount(operatorAccount);
            entity.setMessageType(target.messageType());
            entity.setMessageRefId(target.messageRefId());
            entity.setSnapshotJson("{}");
            entity.setCreatedAt(Instant.now());
            messageReactionRequestRepository.save(entity);
        } catch (Exception e) {
            log.warn("Persist reaction request snapshot failed, requestId={}, operator={}", requestId, operatorAccount, e);
        }
    }

    private MessageReactionSnapshotVO buildSnapshot(MessageTarget target, Long chatId) {
        List<MessageReactionEntity> reactions = messageReactionRepository
                .findByMessageTypeAndMessageRefIdOrderByUpdatedAtDescReactionKeyAsc(target.messageType(), target.messageRefId());
        List<ReactionItemVO> reactionItems = new ArrayList<>();
        for (MessageReactionEntity reactionEntity : reactions) {
            List<MessageReactionUserEntity> users = messageReactionUserRepository
                    .findByMessageTypeAndMessageRefIdAndReactionKeyOrderByUserAccountAsc(
                            target.messageType(), target.messageRefId(), reactionEntity.getReactionKey());
            List<String> userIds = users.stream().map(MessageReactionUserEntity::getUserAccount).toList();
            reactionItems.add(ReactionItemVO.builder()
                    .key(reactionEntity.getReactionKey())
                    .emoji(reactionEntity.getEmoji())
                    .imageUrl(reactionEntity.getImageUrl())
                    .count((long) userIds.size())
                    .userIds(userIds)
                    .updatedAt(reactionEntity.getUpdatedAt())
                    .build());
        }
        return MessageReactionSnapshotVO.builder()
                .chatId(chatId)
                .messageId(target.messageId())
                .serverMessageId(target.messageId())
                .clientMessageId(target.clientMessageId())
                .reactions(reactionItems)
                .build();
    }

    private void publishUpdatedEvent(MessageTarget target, MessageReactionSnapshotVO snapshot) {
        Set<String> recipients = resolveRecipients(target);
        for (String recipient : recipients) {
            MessageReactionUpdatedEventVO event = MessageReactionUpdatedEventVO.builder()
                    .eventType(EVENT_TYPE)
                    .chatId(resolveEventChatId(target, recipient, snapshot.getChatId()))
                    .messageId(target.messageId())
                    .serverMessageId(target.messageId())
                    .updatedAt(Instant.now())
                    .reactions(snapshot.getReactions())
                    .build();
            messagingTemplate.convertAndSendToUser(recipient, EVENT_DESTINATION, event);
        }
    }

    private Long resolveEventChatId(MessageTarget target, String recipient, Long fallbackChatId) {
        if (!TYPE_PRIVATE.equals(target.messageType())) {
            return fallbackChatId;
        }
        if (recipient == null || recipient.isBlank()) {
            return fallbackChatId;
        }
        String peerAccount = recipient.equals(target.fromAccount()) ? target.toAccount() : target.fromAccount();
        Long peerChatId = parsePositiveLong(trim(peerAccount));
        return peerChatId == null ? fallbackChatId : peerChatId;
    }

    private void maybeSendReactionSystemNotify(MessageTarget target,
                                               String operatorAccount,
                                               boolean toggledOn,
                                               String emoji,
                                               String imageUrl,
                                               String reactionKey) {
        if (!TYPE_PRIVATE.equals(target.messageType()) || !toggledOn) {
            return;
        }
        String notifyTo = operatorAccount.equals(target.fromAccount()) ? target.toAccount() : target.fromAccount();
        if (notifyTo == null || notifyTo.equals(operatorAccount)) {
            return;
        }
        var operator = userRepository.findByAccount(operatorAccount);
        String operatorName = operator == null ? operatorAccount : operator.getRealName();
        String operatorAvatarUrl = operator == null ? "" : safe(operator.getAvatarUrl());
        String bubble = buildFullMessageContent(target.messageContent());
        String reactionDisplay = resolveReactionDisplay(emoji, imageUrl, reactionKey);
        String content = "{\"messageContent\":\"" + escapeJson(bubble)
                + "\",\"reaction\":\"" + escapeJson(reactionDisplay)
                + "\",\"operatorName\":\"" + escapeJson(operatorName)
                + "\",\"operatorAvatarUrl\":\"" + escapeJson(operatorAvatarUrl) + "\"}";
        privateMessageDispatchService.dispatchPrivateMessage("SYSTEM", notifyTo, content, null, false);
    }

    private String buildFullMessageContent(String rawContent) {
        if (rawContent == null) {
            return "消息";
        }
        return rawContent;
    }

    private String resolveReactionDisplay(String emoji, String imageUrl, String reactionKey) {
        if (emoji != null && !emoji.isBlank()) {
            return emoji;
        }
        if (imageUrl != null && !imageUrl.isBlank()) {
            return "自定义表情";
        }
        return reactionKey == null || reactionKey.isBlank() ? "表情" : reactionKey;
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private Set<String> resolveRecipients(MessageTarget target) {
        Set<String> accounts = new LinkedHashSet<>();
        if (TYPE_PRIVATE.equals(target.messageType())) {
            accounts.add(target.fromAccount());
            accounts.add(target.toAccount());
            return accounts;
        }
        List<ChatGroupMemberEntity> members = chatGroupMemberRepository.findByGroupIdOrderByJoinedAtAsc(target.groupId());
        for (ChatGroupMemberEntity member : members) {
            accounts.add(member.getUserAccount());
        }
        return accounts;
    }

    private void ensurePermission(MessageTarget target, String operatorAccount) {
        if (TYPE_PRIVATE.equals(target.messageType())) {
            if (!operatorAccount.equals(target.fromAccount()) && !operatorAccount.equals(target.toAccount())) {
                throw new IllegalStateException("无权操作该消息");
            }
            return;
        }
        chatGroupService.ensureMember(target.groupId(), operatorAccount);
    }

    private MessageTarget resolveMessage(String pathMessageId, String serverMessageId, String clientMessageId) {
        String resolvedServerMessageId = trim(serverMessageId);
        if (resolvedServerMessageId == null) {
            resolvedServerMessageId = trim(pathMessageId);
        }
        if (resolvedServerMessageId != null) {
            PrivateMessageEntity privateMessage = privateMessageRepository.findByMessageId(resolvedServerMessageId).orElse(null);
            if (privateMessage != null) {
                return MessageTarget.forPrivate(privateMessage);
            }
            GroupMessageEntity groupMessage = groupMessageRepository.findByMessageId(resolvedServerMessageId).orElse(null);
            if (groupMessage != null) {
                ChatGroupEntity group = chatGroupService.findGroupByNo(groupMessage.getGroupNo());
                return MessageTarget.forGroup(groupMessage, group.getId());
            }
        }

        String resolvedClientMessageId = trim(clientMessageId);
        if (resolvedClientMessageId != null) {
            PrivateMessageEntity privateMessage =
                    privateMessageRepository.findFirstByClientMessageIdOrderBySentAtDesc(resolvedClientMessageId).orElse(null);
            GroupMessageEntity groupMessage =
                    groupMessageRepository.findFirstByClientMessageIdOrderBySentAtDesc(resolvedClientMessageId).orElse(null);
            if (privateMessage != null && groupMessage != null) {
                throw new IllegalStateException("clientMessageId 对应多条消息，请传入 serverMessageId");
            }
            if (privateMessage != null) {
                return MessageTarget.forPrivate(privateMessage);
            }
            if (groupMessage != null) {
                ChatGroupEntity group = chatGroupService.findGroupByNo(groupMessage.getGroupNo());
                return MessageTarget.forGroup(groupMessage, group.getId());
            }
        }

        Long localMessagePk = parsePositiveLong(trim(pathMessageId));
        if (localMessagePk != null) {
            PrivateMessageEntity privateMessage = privateMessageRepository.findById(localMessagePk).orElse(null);
            if (privateMessage != null) {
                return MessageTarget.forPrivate(privateMessage);
            }
            GroupMessageEntity groupMessage = groupMessageRepository.findById(localMessagePk).orElse(null);
            if (groupMessage != null) {
                ChatGroupEntity group = chatGroupService.findGroupByNo(groupMessage.getGroupNo());
                return MessageTarget.forGroup(groupMessage, group.getId());
            }
        }
        throw new IllegalArgumentException("消息不存在");
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String result = value.trim();
        return result.isEmpty() ? null : result;
    }

    private Long parsePositiveLong(String value) {
        if (value == null) {
            return null;
        }
        try {
            long parsed = Long.parseLong(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private record MessageTarget(String messageType,
                                 String messageRefId,
                                 String messageId,
                                 String clientMessageId,
                                 String fromAccount,
                                 String toAccount,
                                 Long groupId,
                                 String messageContent) {
        private static MessageTarget forPrivate(PrivateMessageEntity message) {
            return new MessageTarget(TYPE_PRIVATE,
                    message.getMessageId(),
                    message.getMessageId(),
                    message.getClientMessageId(),
                    message.getFromAccount(),
                    message.getToAccount(),
                    null,
                    message.getContent());
        }

        private static MessageTarget forGroup(GroupMessageEntity message, Long groupId) {
            return new MessageTarget(TYPE_GROUP,
                    message.getMessageId(),
                    message.getMessageId(),
                    message.getClientMessageId(),
                    null,
                    null,
                    groupId,
                    message.getContent());
        }
    }
}
