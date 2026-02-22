package com.lujianfeng.spanner.service;

import com.lujianfeng.spanner.entity.group.ChatGroupEntity;
import com.lujianfeng.spanner.entity.group.ChatGroupMemberEntity;
import com.lujianfeng.spanner.entity.message.GroupMessageEntity;
import com.lujianfeng.spanner.repository.ChatGroupMemberRepository;
import com.lujianfeng.spanner.repository.GroupMessageRepository;
import com.lujianfeng.spanner.vo.message.GroupMessageAckVO;
import com.lujianfeng.spanner.vo.message.GroupMessageVO;
import com.lujianfeng.spanner.vo.message.MessageQuoteVO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class GroupMessageDispatchService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatGroupService chatGroupService;
    private final ChatGroupMemberRepository chatGroupMemberRepository;
    private final GroupMessageRepository groupMessageRepository;

    public GroupMessageDispatchService(SimpMessagingTemplate messagingTemplate,
                                       ChatGroupService chatGroupService,
                                       ChatGroupMemberRepository chatGroupMemberRepository,
                                       GroupMessageRepository groupMessageRepository) {
        this.messagingTemplate = messagingTemplate;
        this.chatGroupService = chatGroupService;
        this.chatGroupMemberRepository = chatGroupMemberRepository;
        this.groupMessageRepository = groupMessageRepository;
    }

    public GroupMessageAckVO dispatchGroupMessage(String from,
                                                  String groupNo,
                                                  String content,
                                                  String clientMessageId) {
        return dispatchGroupMessage(from, groupNo, content, null, clientMessageId);
    }

    public GroupMessageAckVO dispatchGroupMessage(String from,
                                                  String groupNo,
                                                  String content,
                                                  MessageQuoteVO quote,
                                                  String clientMessageId) {
        ChatGroupEntity group = chatGroupService.findGroupByNo(groupNo);
        chatGroupService.ensureMember(group.getId(), from);

        String messageId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        GroupMessageVO messageVO = GroupMessageVO.builder()
                .messageId(messageId)
                .groupNo(group.getGroupNo())
                .from(from)
                .content(content)
                .quote(quote)
                .clientMessageId(clientMessageId)
                .sentAt(now)
                .build();

        persistMessage(messageVO);
        List<ChatGroupMemberEntity> members = chatGroupMemberRepository.findByGroupIdOrderByJoinedAtAsc(group.getId());
        for (ChatGroupMemberEntity member : members) {
            messagingTemplate.convertAndSendToUser(member.getUserAccount(), "/queue/group.messages", messageVO);
        }

        return GroupMessageAckVO.builder()
                .clientMessageId(clientMessageId)
                .messageId(messageId)
                .groupNo(group.getGroupNo())
                .status("SENT")
                .ackAt(now)
                .build();
    }

    private void persistMessage(GroupMessageVO messageVO) {
        GroupMessageEntity entity = new GroupMessageEntity();
        entity.setMessageId(messageVO.getMessageId());
        entity.setGroupNo(messageVO.getGroupNo());
        entity.setFromAccount(messageVO.getFrom());
        entity.setContent(messageVO.getContent());
        entity.setQuotedMessageId(messageVO.getQuote() == null ? null : messageVO.getQuote().getMessageId());
        entity.setQuotedFromAccount(messageVO.getQuote() == null ? null : messageVO.getQuote().getFrom());
        entity.setQuotedContent(messageVO.getQuote() == null ? null : messageVO.getQuote().getContent());
        entity.setClientMessageId(messageVO.getClientMessageId());
        entity.setSentAt(messageVO.getSentAt() == null ? LocalDateTime.now() : messageVO.getSentAt());
        groupMessageRepository.save(entity);
    }
}
