package com.lujianfeng.spanner.service;

import com.lujianfeng.spanner.vo.message.PrivateMessageVO;
import com.lujianfeng.spanner.vo.message.MessageQuoteVO;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 离线私聊消息存储与提取服务
 */
@Service
public class OfflineMessageService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OfflineMessageService.class);
    private static final String OFFLINE_MESSAGE_KEY_PREFIX = "ws:offline:private:";
    private static final Duration OFFLINE_MESSAGE_TTL = Duration.ofDays(7);

    private final RedisTemplate<String, Object> redisTemplate;

    public OfflineMessageService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean storePrivateMessage(String account, PrivateMessageVO message) {
        if (isBlank(account) || message == null) {
            return false;
        }

        String key = buildKey(account);
        try {
            Long queueSize = redisTemplate.opsForList().rightPush(key, message);
            redisTemplate.expire(key, OFFLINE_MESSAGE_TTL);
            log.debug("Stored offline private message, account={}, messageId={}, queueSize={}",
                    account, message.getMessageId(), queueSize);
            return true;
        } catch (DataAccessException e) {
            log.error("Failed to store offline private message due to Redis error, account={}, messageId={}",
                    account, message.getMessageId(), e);
            return false;
        }
    }

    public List<PrivateMessageVO> drainPrivateMessages(String account) {
        if (isBlank(account)) {
            return List.of();
        }

        String key = buildKey(account);
        try {
            List<Object> payloads = redisTemplate.opsForList().range(key, 0, -1);
            if (payloads == null || payloads.isEmpty()) {
                return List.of();
            }
            redisTemplate.delete(key);

            List<PrivateMessageVO> result = new ArrayList<>(payloads.size());
            for (Object payload : payloads) {
                if (payload instanceof PrivateMessageVO message) {
                    result.add(message);
                } else if (payload != null) {
                    PrivateMessageVO mapped = mapToPrivateMessage(payload);
                    if (mapped != null) {
                        result.add(mapped);
                    } else {
                        log.warn("Drop unexpected offline message payload type, account={}, payloadType={}",
                                account, payload.getClass().getName());
                    }
                } else {
                    log.warn("Drop unexpected offline message payload type, account={}, payloadType={}",
                            account, "null");
                }
            }
            return result;
        } catch (DataAccessException e) {
            log.error("Failed to drain offline private messages due to Redis error, account={}", account, e);
            safeDeleteCorruptedQueue(key, account);
            return List.of();
        }
    }

    private void safeDeleteCorruptedQueue(String key, String account) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.warn("Deleted unreadable offline message queue, account={}, deleted={}", account, deleted);
        } catch (Exception deleteError) {
            log.error("Failed to delete unreadable offline message queue, account={}", account, deleteError);
        }
    }

    private String buildKey(String account) {
        return OFFLINE_MESSAGE_KEY_PREFIX + account;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private PrivateMessageVO mapToPrivateMessage(Object payload) {
        if (!(payload instanceof Map<?, ?> map)) {
            return null;
        }
        return PrivateMessageVO.builder()
                .messageId(asString(map.get("messageId")))
                .from(asString(map.get("from")))
                .to(asString(map.get("to")))
                .content(asString(map.get("content")))
                .quote(asQuote(map.get("quote")))
                .clientMessageId(asString(map.get("clientMessageId")))
                .sentAt(asLocalDateTime(map.get("sentAt")))
                .build();
    }

    private MessageQuoteVO asQuote(Object value) {
        if (!(value instanceof Map<?, ?> quoteMap)) {
            return null;
        }
        String messageId = asString(quoteMap.get("messageId"));
        if (messageId == null || messageId.isBlank()) {
            return null;
        }
        return MessageQuoteVO.builder()
                .messageId(messageId)
                .from(asString(quoteMap.get("from")))
                .content(asString(quoteMap.get("content")))
                .build();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private LocalDateTime asLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        try {
            return LocalDateTime.parse(String.valueOf(value));
        } catch (Exception ignore) {
            return null;
        }
    }
}
