package com.lujianfeng.spanner.controller;

import com.lujianfeng.spanner.dto.message.MessageDTO;
import com.lujianfeng.spanner.dto.message.PrivateMessageSendDTO;
import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.entity.user.UserRelationEnum;
import com.lujianfeng.spanner.repository.UserRelationRepository;
import com.lujianfeng.spanner.repository.UserRepository;
import com.lujianfeng.spanner.service.PrivateMessageDispatchService;
import com.lujianfeng.spanner.vo.message.MessageAckVO;
import com.lujianfeng.spanner.vo.message.WsErrorVO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;

/**
 * Chat Controller
 *
 * @author Lujianfeng
 * @version 1.0
 * @date 2025/12/15
 * @since 1.0
 */

@Controller
public class ChatController {
    private static final Logger log = LogManager.getLogger(ChatController.class);
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final UserRelationRepository userRelationRepository;
    private final PrivateMessageDispatchService privateMessageDispatchService;

    public ChatController(SimpMessagingTemplate messagingTemplate,
                          UserRepository userRepository,
                          UserRelationRepository userRelationRepository,
                          PrivateMessageDispatchService privateMessageDispatchService) {
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
        this.userRelationRepository = userRelationRepository;
        this.privateMessageDispatchService = privateMessageDispatchService;
    }

    @MessageMapping("chat.send")
    @SendTo("/topic/public")
    public MessageDTO sendMessage(MessageDTO message) {
        if (message == null) {
            MessageDTO fallback = new MessageDTO();
            fallback.setContent("Server Return:");
            return fallback;
        }
        log.info("Received the MessageDTO{}", message);
        log.info("Received the Message Content:{}", message.getContent());
        String content = message.getContent() == null ? "" : message.getContent();
        message.setContent("Server Return:" + content);

        return message;
    }

    /**
     * 用户私聊发送
     */
    @MessageMapping("chat/private.send")
    public void sendPrivateMessage(@Payload PrivateMessageSendDTO payload, Principal principal) {
        String from = principal == null ? null : principal.getName();
        String clientMessageId = payload == null ? null : trim(payload.getClientMessageId());
        if (from == null || from.isBlank()) {
            log.warn("Drop private message because principal is missing");
            return;
        }

        try {
            String to = payload == null ? null : trim(payload.getTo());
            String content = payload == null ? null : trim(payload.getContent());
            log.debug("Received private message, from={}, to={}, clientMessageId={}", from, to, clientMessageId);

            if (to == null) {
                log.debug("Reject private message from={}, reason=INVALID_TO", from);
                sendErrorToUser(from, "INVALID_TO", "to 不能为空", clientMessageId);
                return;
            }
            if (content == null) {
                log.debug("Reject private message from={}, reason=INVALID_CONTENT", from);
                sendErrorToUser(from, "INVALID_CONTENT", "content 不能为空", clientMessageId);
                return;
            }

            UserEntity fromUser = userRepository.findByAccount(from);
            UserEntity toUser = userRepository.findByAccount(to);
            if (fromUser == null) {
                log.debug("Reject private message from={}, reason=SENDER_NOT_FOUND", from);
                sendErrorToUser(from, "SENDER_NOT_FOUND", "发送方账号不存在", clientMessageId);
                return;
            }
            if (toUser == null) {
                log.debug("Reject private message from={}, to={}, reason=TARGET_NOT_FOUND", from, to);
                sendErrorToUser(from, "TARGET_NOT_FOUND", "目标用户不存在", clientMessageId);
                return;
            }
            if (!isFriend(fromUser, toUser)) {
                log.debug("Reject private message from={}, to={}, reason=NOT_FRIEND", from, to);
                sendErrorToUser(from, "NOT_FRIEND", "仅支持好友之间发送私聊消息", clientMessageId);
                return;
            }

            MessageAckVO ackVO = privateMessageDispatchService.dispatchPrivateMessage(from, to, content, clientMessageId, true);
            messagingTemplate.convertAndSendToUser(from, "/queue/acks", ackVO);
            log.debug("Sent private message ack, messageId={}, from={}, to={}, status={}",
                    ackVO.getMessageId(), from, to, ackVO.getStatus());
        } catch (Exception e) {
            log.error("Unhandled error while sending private message, from={}, clientMessageId={}",
                    from, clientMessageId, e);
            sendErrorToUser(from, "INTERNAL_ERROR", "消息发送失败，请稍后重试", clientMessageId);
        }
    }

    private void sendErrorToUser(String account, String code, String message, String clientMessageId) {
        WsErrorVO error = WsErrorVO.builder()
                .code(code)
                .message(message)
                .clientMessageId(clientMessageId)
                .at(LocalDateTime.now())
                .build();
        messagingTemplate.convertAndSendToUser(account, "/queue/errors", error);
    }

    private boolean isFriend(UserEntity fromUser, UserEntity toUser) {
        return userRelationRepository.existsByUserAndFriendAndRelationType(fromUser, toUser, UserRelationEnum.ACCEPTED)
                || userRelationRepository.existsByUserAndFriendAndRelationType(toUser, fromUser, UserRelationEnum.ACCEPTED);
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String result = value.trim();
        return result.isEmpty() ? null : result;
    }

}
