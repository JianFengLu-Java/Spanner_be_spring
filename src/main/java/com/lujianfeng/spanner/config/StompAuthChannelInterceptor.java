package com.lujianfeng.spanner.config;

import com.lujianfeng.spanner.util.JwtUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * STOMP CONNECT 鉴权并绑定用户身份
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public StompAuthChannelInterceptor(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(command)) {
            authenticateOnConnect(accessor);
            return message;
        }

        // 除 CONNECT 外，所有帧都要求已绑定用户身份
        if (accessor.getUser() == null || accessor.getUser().getName() == null || accessor.getUser().getName().isBlank()) {
            throw new MessagingException("WebSocket 会话未认证，请先携带 token 连接");
        }

        if (StompCommand.SUBSCRIBE.equals(command)) {
            String destination = accessor.getDestination();
            if (destination == null || !destination.startsWith("/user/queue/")) {
                throw new MessagingException("仅允许订阅 /user/queue/** 通道");
            }
        }
        return message;
    }

    private void authenticateOnConnect(StompHeaderAccessor accessor) {
        String token = resolveToken(accessor);
        if (token == null) {
            throw new MessagingException("WebSocket 连接失败：缺少 Authorization token");
        }

        String account;
        try {
            account = jwtUtil.extractUsername(token);
        } catch (Exception e) {
            throw new MessagingException("WebSocket 连接失败：token 非法");
        }
        if (account == null || account.isBlank()) {
            throw new MessagingException("WebSocket 连接失败：token 已过期");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(account);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        accessor.setUser(authentication);
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String auth = firstNativeHeader(accessor, "Authorization", "authorization");
        if (auth != null) {
            return trimBearerPrefix(auth);
        }

        String token = firstNativeHeader(accessor, "token", "Token");
        return trimBearerPrefix(token);
    }

    private String firstNativeHeader(StompHeaderAccessor accessor, String... candidates) {
        for (String candidate : candidates) {
            List<String> values = accessor.getNativeHeader(candidate);
            if (values != null && !values.isEmpty()) {
                return values.get(0);
            }
        }
        return null;
    }

    private String trimBearerPrefix(String raw) {
        if (raw == null) {
            return null;
        }
        String token = raw.trim();
        if (token.startsWith("Bearer ")) {
            return token.substring(7).trim();
        }
        return token.isEmpty() ? null : token;
    }
}
