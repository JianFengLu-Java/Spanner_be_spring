package com.lujianfeng.spanner.controller;

import com.lujianfeng.spanner.entity.message.PrivateMessageEntity;
import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.entity.user.UserRelationEnum;
import com.lujianfeng.spanner.repository.PrivateMessageRepository;
import com.lujianfeng.spanner.repository.UserRelationRepository;
import com.lujianfeng.spanner.repository.UserRepository;
import com.lujianfeng.spanner.service.OfflineMessageService;
import com.lujianfeng.spanner.service.service.UserService;
import com.lujianfeng.spanner.vo.message.MessageQuoteVO;
import com.lujianfeng.spanner.vo.message.PrivateMessageVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 登录后离线消息拉取接口
 */
@RestController
@RequestMapping("/messages")
public class OfflineMessageController {

    private final UserService userService;
    private final OfflineMessageService offlineMessageService;
    private final UserRepository userRepository;
    private final UserRelationRepository userRelationRepository;
    private final PrivateMessageRepository privateMessageRepository;

    public OfflineMessageController(UserService userService,
                                    OfflineMessageService offlineMessageService,
                                    UserRepository userRepository,
                                    UserRelationRepository userRelationRepository,
                                    PrivateMessageRepository privateMessageRepository) {
        this.userService = userService;
        this.offlineMessageService = offlineMessageService;
        this.userRepository = userRepository;
        this.userRelationRepository = userRelationRepository;
        this.privateMessageRepository = privateMessageRepository;
    }

    /**
     * 拉取并清空当前登录用户的离线私聊消息
     */
    @GetMapping("/offline")
    public ResponseEntity<Map<String, Object>> pullOfflineMessages() {
        UserEntity currentUser = userService.getCurrentUserEntity();
        if (currentUser == null || currentUser.getAccount() == null || currentUser.getAccount().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, "未登录"));
        }

        List<PrivateMessageVO> messages = offlineMessageService.drainPrivateMessages(currentUser.getAccount());
        messages = enrichMessageNames(messages);
        Map<String, Object> data = new HashMap<>();
        data.put("messages", messages);
        data.put("count", messages.size());
        data.put("pulledAt", LocalDateTime.now());

        return ResponseEntity.ok(success("拉取离线消息成功", data));
    }

    /**
     * 分页查询当前用户与指定好友的历史私聊消息
     */
    @GetMapping("/history/{friendAccount}")
    public ResponseEntity<Map<String, Object>> getHistory(@PathVariable String friendAccount,
                                                          @RequestParam(defaultValue = "1") Integer page,
                                                          @RequestParam(defaultValue = "20") Integer size) {
        UserEntity currentUser = userService.getCurrentUserEntity();
        if (currentUser == null || currentUser.getAccount() == null || currentUser.getAccount().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, "未登录"));
        }
        if (friendAccount == null || friendAccount.isBlank()) {
            return ResponseEntity.badRequest().body(error(400, "friendAccount 不能为空"));
        }

        String currentAccount = currentUser.getAccount();
        String targetAccount = friendAccount.trim();
        UserEntity targetUser = userRepository.findByAccount(targetAccount);
        if (targetUser == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(404, "目标用户不存在"));
        }
        if (!isFriend(currentUser, targetUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(403, "仅支持查询好友之间的聊天记录"));
        }

        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize);

        Page<PrivateMessageEntity> pageResult =
                privateMessageRepository.findByFromAccountAndToAccountOrFromAccountAndToAccountOrderBySentAtDesc(
                        currentAccount, targetAccount, targetAccount, currentAccount, pageable);

        List<PrivateMessageVO> messages = new ArrayList<>(pageResult.getContent().stream()
                .map(entity -> toVO(entity, null))
                .toList());
        messages = enrichMessageNames(messages);
        Collections.reverse(messages);

        Map<String, Object> data = new HashMap<>();
        data.put("messages", messages);
        data.put("page", safePage);
        data.put("size", safeSize);
        data.put("total", pageResult.getTotalElements());
        data.put("totalPages", pageResult.getTotalPages());
        data.put("hasMore", pageResult.hasNext());
        return ResponseEntity.ok(success("查询聊天记录成功", data));
    }

    private Map<String, Object> success(String message, Object data) {
        return Map.of(
                "code", 200,
                "status", "success",
                "message", message,
                "data", data == null ? Map.of() : data
        );
    }

    private Map<String, Object> error(int code, String message) {
        return Map.of(
                "code", code,
                "status", "error",
                "message", message
        );
    }

    private boolean isFriend(UserEntity fromUser, UserEntity toUser) {
        return userRelationRepository.existsByUserAndFriendAndRelationType(fromUser, toUser, UserRelationEnum.ACCEPTED)
                || userRelationRepository.existsByUserAndFriendAndRelationType(toUser, fromUser, UserRelationEnum.ACCEPTED);
    }

    private PrivateMessageVO toVO(PrivateMessageEntity entity, String fromRealName) {
        return PrivateMessageVO.builder()
                .messageId(entity.getMessageId())
                .from(entity.getFromAccount())
                .formName(fromRealName)
                .fromName(fromRealName)
                .fromRealName(fromRealName)
                .fromAvatarUrl(null)
                .to(entity.getToAccount())
                .content(entity.getContent())
                .quote(toQuoteVO(entity.getQuotedMessageId(),
                        entity.getQuotedFromAccount(),
                        entity.getQuotedContent(),
                        null,
                        null))
                .clientMessageId(entity.getClientMessageId())
                .sentAt(entity.getSentAt())
                .recalled(Boolean.TRUE.equals(entity.getRecalled()))
                .recalledAt(entity.getRecalledAt())
                .build();
    }

    private MessageQuoteVO toQuoteVO(String messageId, String from, String content, String fromRealName, String fromAvatarUrl) {
        if (messageId == null || messageId.isBlank()) {
            return null;
        }
        return MessageQuoteVO.builder()
                .messageId(messageId)
                .from(from)
                .formName(fromRealName)
                .fromName(fromRealName)
                .fromRealName(fromRealName)
                .fromAvatarUrl(fromAvatarUrl)
                .content(content)
                .build();
    }

    private List<PrivateMessageVO> enrichMessageNames(List<PrivateMessageVO> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        Set<String> accounts = new HashSet<>();
        for (PrivateMessageVO message : messages) {
            if (message.getFrom() != null && !message.getFrom().isBlank()) {
                accounts.add(message.getFrom());
            }
            if (message.getQuote() != null && message.getQuote().getFrom() != null && !message.getQuote().getFrom().isBlank()) {
                accounts.add(message.getQuote().getFrom());
            }
        }
        Map<String, UserProfile> profileMap = loadUserProfileMap(accounts);
        List<PrivateMessageVO> result = new ArrayList<>(messages.size());
        for (PrivateMessageVO message : messages) {
            UserProfile fromProfile = resolveProfile(profileMap, message.getFrom());
            MessageQuoteVO quote = message.getQuote();
            UserProfile quoteProfile = quote == null ? null : resolveProfile(profileMap, quote.getFrom());
            MessageQuoteVO quoteWithName = quote == null ? null : MessageQuoteVO.builder()
                    .messageId(quote.getMessageId())
                    .from(quote.getFrom())
                    .formName(quoteProfile == null ? null : quoteProfile.realName())
                    .fromName(quoteProfile == null ? null : quoteProfile.realName())
                    .fromRealName(quoteProfile == null ? null : quoteProfile.realName())
                    .fromAvatarUrl(firstNonBlank(quote.getFromAvatarUrl(), quoteProfile == null ? null : quoteProfile.avatarUrl()))
                    .content(quote.getContent())
                    .build();
            result.add(PrivateMessageVO.builder()
                    .messageId(message.getMessageId())
                    .from(message.getFrom())
                    .formName(fromProfile.realName())
                    .fromName(fromProfile.realName())
                    .fromRealName(fromProfile.realName())
                    .fromAvatarUrl(firstNonBlank(message.getFromAvatarUrl(), fromProfile.avatarUrl()))
                    .to(message.getTo())
                    .content(message.getContent())
                    .quote(quoteWithName)
                    .clientMessageId(message.getClientMessageId())
                    .sentAt(message.getSentAt())
                    .recalled(Boolean.TRUE.equals(message.getRecalled()))
                    .recalledAt(message.getRecalledAt())
                    .build());
        }
        return result;
    }

    private Map<String, UserProfile> loadUserProfileMap(Collection<String> accounts) {
        if (accounts == null || accounts.isEmpty()) {
            return Map.of();
        }
        Map<String, UserProfile> map = new HashMap<>();
        for (UserEntity user : userRepository.findByAccountIn(accounts)) {
            String realName = user.getRealName();
            if (realName == null || realName.isBlank()) {
                realName = user.getAccount();
            }
            map.put(user.getAccount(), new UserProfile(realName, user.getAvatarUrl()));
        }
        return map;
    }

    private UserProfile resolveProfile(Map<String, UserProfile> map, String account) {
        if (account == null || account.isBlank()) {
            return new UserProfile(null, null);
        }
        UserProfile profile = map.get(account);
        if (profile != null) {
            return profile;
        }
        return new UserProfile(account, null);
    }

    private record UserProfile(String realName, String avatarUrl) {
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
