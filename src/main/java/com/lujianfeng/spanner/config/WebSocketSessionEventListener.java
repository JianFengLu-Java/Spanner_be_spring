package com.lujianfeng.spanner.config;
import com.lujianfeng.spanner.service.CloudDocCollabWsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
/**
 * WebSocket 会话事件监听
 */
@Component
public class WebSocketSessionEventListener {
    private static final Logger log = LoggerFactory.getLogger(WebSocketSessionEventListener.class);
    private final CloudDocCollabWsService cloudDocCollabWsService;

    public WebSocketSessionEventListener(CloudDocCollabWsService cloudDocCollabWsService) {
        this.cloudDocCollabWsService = cloudDocCollabWsService;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        String account = user == null ? null : user.getName();
        log.info("WebSocket connected, sessionId={}, user={}", accessor.getSessionId(),
                account == null ? "anonymous" : account);

        // 离线消息改为登录后 REST 主动拉取，避免连接建立时订阅尚未完成导致前端丢消息。
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        cloudDocCollabWsService.handleDisconnect(accessor.getSessionId());
        log.info("WebSocket disconnected, sessionId={}, user={}",
                accessor.getSessionId(),
                user == null ? "anonymous" : user.getName());
    }
}
