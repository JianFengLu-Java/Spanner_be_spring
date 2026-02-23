package com.lujianfeng.spanner.service;

import com.lujianfeng.spanner.dto.message.MessageRecallRequestDTO;
import com.lujianfeng.spanner.entity.group.ChatGroupEntity;
import com.lujianfeng.spanner.entity.group.ChatGroupMemberEntity;
import com.lujianfeng.spanner.entity.message.GroupMessageEntity;
import com.lujianfeng.spanner.entity.message.PrivateMessageEntity;
import com.lujianfeng.spanner.repository.ChatGroupMemberRepository;
import com.lujianfeng.spanner.repository.GroupMessageRepository;
import com.lujianfeng.spanner.repository.PrivateMessageRepository;
import com.lujianfeng.spanner.vo.message.MessageRecalledEventVO;
import com.lujianfeng.spanner.vo.message.MessageRecallResultVO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class MessageRecallService {

    private static final Duration RECALL_WINDOW = Duration.ofDays(1);
    private static final String RECALLED_CONTENT = "该消息已撤回";

    private final PrivateMessageRepository privateMessageRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final ChatGroupService chatGroupService;
    private final ChatGroupMemberRepository chatGroupMemberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageRecallService(PrivateMessageRepository privateMessageRepository,
                                GroupMessageRepository groupMessageRepository,
                                ChatGroupService chatGroupService,
                                ChatGroupMemberRepository chatGroupMemberRepository,
                                SimpMessagingTemplate messagingTemplate) {
        this.privateMessageRepository = privateMessageRepository;
        this.groupMessageRepository = groupMessageRepository;
        this.chatGroupService = chatGroupService;
        this.chatGroupMemberRepository = chatGroupMemberRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public MessageRecallResultVO recall(String account, String messageId, MessageRecallRequestDTO request) {
        String operator = trim(account);
        if (operator == null) {
            throw new IllegalArgumentException("未登录");
        }
        String refId = trim(messageId);
        if (refId == null) {
            throw new IllegalArgumentException("messageId 不能为空");
        }

        String messageType = normalizeType(request == null ? null : request.getMessageType());
        if ("PRIVATE".equals(messageType)) {
            return recallPrivate(operator, refId);
        }
        if ("GROUP".equals(messageType)) {
            return recallGroup(operator, refId, request == null ? null : request.getGroupNo());
        }
        throw new IllegalArgumentException("messageType 仅支持 PRIVATE/GROUP");
    }

    private MessageRecallResultVO recallPrivate(String operator, String messageId) {
        PrivateMessageEntity message = privateMessageRepository.findByMessageId(messageId)
                .orElseThrow(() -> new IllegalArgumentException("消息不存在"));
        if (!operator.equals(message.getFromAccount())) {
            throw new IllegalStateException("仅支持撤回自己发送的消息");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = safeDeadline(message.getSentAt());
        validateRecallable(message.getRecalled(), deadline, now);

        message.setRecalled(true);
        message.setRecalledAt(now);
        message.setContent(RECALLED_CONTENT);
        message.setQuotedMessageId(null);
        message.setQuotedFromAccount(null);
        message.setQuotedContent(null);
        privateMessageRepository.save(message);

        MessageRecalledEventVO event = MessageRecalledEventVO.builder()
                .eventType("MESSAGE_RECALLED")
                .messageType("PRIVATE")
                .messageId(message.getMessageId())
                .from(message.getFromAccount())
                .to(message.getToAccount())
                .recalled(true)
                .recalledAt(now)
                .build();
        messagingTemplate.convertAndSendToUser(message.getFromAccount(), "/queue/messages.recalled", event);
        if (message.getToAccount() != null && !message.getToAccount().isBlank()
                && !message.getToAccount().equals(message.getFromAccount())) {
            messagingTemplate.convertAndSendToUser(message.getToAccount(), "/queue/messages.recalled", event);
        }

        return MessageRecallResultVO.builder()
                .messageId(message.getMessageId())
                .messageType("PRIVATE")
                .from(message.getFromAccount())
                .to(message.getToAccount())
                .recalled(true)
                .recalledAt(now)
                .recallDeadlineAt(deadline)
                .build();
    }

    private MessageRecallResultVO recallGroup(String operator, String messageId, String groupNoRaw) {
        GroupMessageEntity message = groupMessageRepository.findByMessageId(messageId)
                .orElseThrow(() -> new IllegalArgumentException("消息不存在"));
        if (!operator.equals(message.getFromAccount())) {
            throw new IllegalStateException("仅支持撤回自己发送的消息");
        }

        String groupNo = trim(groupNoRaw);
        if (groupNo != null && !groupNo.equals(message.getGroupNo())) {
            throw new IllegalArgumentException("groupNo 与消息不匹配");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = safeDeadline(message.getSentAt());
        validateRecallable(message.getRecalled(), deadline, now);

        message.setRecalled(true);
        message.setRecalledAt(now);
        message.setContent(RECALLED_CONTENT);
        message.setQuotedMessageId(null);
        message.setQuotedFromAccount(null);
        message.setQuotedContent(null);
        groupMessageRepository.save(message);

        ChatGroupEntity group = chatGroupService.findGroupByNo(message.getGroupNo());
        List<ChatGroupMemberEntity> members = chatGroupMemberRepository.findByGroupIdOrderByJoinedAtAsc(group.getId());

        MessageRecalledEventVO event = MessageRecalledEventVO.builder()
                .eventType("MESSAGE_RECALLED")
                .messageType("GROUP")
                .messageId(message.getMessageId())
                .groupNo(message.getGroupNo())
                .from(message.getFromAccount())
                .recalled(true)
                .recalledAt(now)
                .build();

        for (ChatGroupMemberEntity member : members) {
            messagingTemplate.convertAndSendToUser(member.getUserAccount(), "/queue/group.messages.recalled", event);
        }

        return MessageRecallResultVO.builder()
                .messageId(message.getMessageId())
                .messageType("GROUP")
                .groupNo(message.getGroupNo())
                .from(message.getFromAccount())
                .recalled(true)
                .recalledAt(now)
                .recallDeadlineAt(deadline)
                .build();
    }

    private void validateRecallable(Boolean recalled, LocalDateTime deadline, LocalDateTime now) {
        if (Boolean.TRUE.equals(recalled)) {
            throw new IllegalStateException("消息已撤回");
        }
        if (deadline != null && now.isAfter(deadline)) {
            throw new IllegalStateException("消息发送超过24小时，无法撤回");
        }
    }

    private LocalDateTime safeDeadline(LocalDateTime sentAt) {
        return sentAt == null ? null : sentAt.plus(RECALL_WINDOW);
    }

    private String normalizeType(String raw) {
        String value = trim(raw);
        if (value == null) {
            return null;
        }
        return value.toUpperCase(Locale.ROOT);
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String result = value.trim();
        return result.isEmpty() ? null : result;
    }
}
