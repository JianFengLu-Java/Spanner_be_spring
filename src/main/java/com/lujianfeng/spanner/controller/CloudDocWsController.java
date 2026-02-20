package com.lujianfeng.spanner.controller;

import com.lujianfeng.spanner.dto.cloud.CloudDocWsCursorDTO;
import com.lujianfeng.spanner.dto.cloud.CloudDocWsJoinDTO;
import com.lujianfeng.spanner.dto.cloud.CloudDocWsLeaveDTO;
import com.lujianfeng.spanner.dto.cloud.CloudDocWsPatchDTO;
import com.lujianfeng.spanner.service.CloudDocCollabWsService;
import com.lujianfeng.spanner.vo.cloud.CloudDocWsAckVO;
import com.lujianfeng.spanner.vo.message.WsErrorVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
public class CloudDocWsController {

    private static final Logger log = LoggerFactory.getLogger(CloudDocWsController.class);

    private final CloudDocCollabWsService cloudDocCollabWsService;
    private final SimpMessagingTemplate messagingTemplate;

    public CloudDocWsController(CloudDocCollabWsService cloudDocCollabWsService,
                                SimpMessagingTemplate messagingTemplate) {
        this.cloudDocCollabWsService = cloudDocCollabWsService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("cloud-docs.join")
    public void join(@Payload CloudDocWsJoinDTO payload, Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        String account = currentAccount(principal);
        String sessionId = headerAccessor == null ? null : headerAccessor.getSessionId();
        try {
            CloudDocWsAckVO ack = cloudDocCollabWsService.join(account, sessionId, payload == null ? null : payload.getDocId());
            sendAck(account, ack);
        } catch (Exception e) {
            sendError(account, "CLOUD_DOC_WS_JOIN_FAILED", e.getMessage(), null);
        }
    }

    @MessageMapping("cloud-docs.leave")
    public void leave(@Payload CloudDocWsLeaveDTO payload, Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        String account = currentAccount(principal);
        String sessionId = headerAccessor == null ? null : headerAccessor.getSessionId();
        try {
            CloudDocWsAckVO ack = cloudDocCollabWsService.leave(account, sessionId, payload == null ? null : payload.getDocId());
            sendAck(account, ack);
        } catch (Exception e) {
            sendError(account, "CLOUD_DOC_WS_LEAVE_FAILED", e.getMessage(), null);
        }
    }

    @MessageMapping("cloud-docs.cursor")
    public void cursor(@Payload CloudDocWsCursorDTO payload, Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        String account = currentAccount(principal);
        String sessionId = headerAccessor == null ? null : headerAccessor.getSessionId();
        try {
            CloudDocWsAckVO ack = cloudDocCollabWsService.cursor(account, sessionId, payload);
            sendAck(account, ack);
        } catch (Exception e) {
            sendError(account, "CLOUD_DOC_WS_CURSOR_FAILED", e.getMessage(), null);
        }
    }

    @MessageMapping("cloud-docs.patch")
    public void patch(@Payload CloudDocWsPatchDTO payload, Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        String account = currentAccount(principal);
        String sessionId = headerAccessor == null ? null : headerAccessor.getSessionId();
        try {
            CloudDocWsAckVO ack = cloudDocCollabWsService.patch(account, sessionId, payload);
            sendAck(account, ack);
        } catch (Exception e) {
            sendError(account, "CLOUD_DOC_WS_PATCH_FAILED", e.getMessage(), payload == null ? null : payload.getOpId());
        }
    }

    private String currentAccount(Principal principal) {
        String account = principal == null ? null : principal.getName();
        if (account == null || account.isBlank()) {
            throw new IllegalStateException("未登录");
        }
        return account;
    }

    private void sendAck(String account, CloudDocWsAckVO ack) {
        messagingTemplate.convertAndSendToUser(account, "/queue/cloud-docs.acks", ack);
    }

    private void sendError(String account, String code, String message, String clientMessageId) {
        try {
            if (account == null || account.isBlank()) {
                return;
            }
            WsErrorVO error = WsErrorVO.builder()
                    .code(code)
                    .message(message == null || message.isBlank() ? "操作失败" : message)
                    .clientMessageId(clientMessageId)
                    .at(LocalDateTime.now())
                    .build();
            messagingTemplate.convertAndSendToUser(account, "/queue/errors", error);
        } catch (Exception ex) {
            log.warn("send cloud-doc ws error failed", ex);
        }
    }
}
