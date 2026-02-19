package com.lujianfeng.spanner.controller;

import com.lujianfeng.spanner.dto.user.FriendActionRequestDTO;
import com.lujianfeng.spanner.dto.user.FriendCancelRequestDTO;
import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.entity.user.UserRelation;
import com.lujianfeng.spanner.repository.UserRelationRepository;
import com.lujianfeng.spanner.repository.UserRepository;
import com.lujianfeng.spanner.service.service.UserRelationService;
import com.lujianfeng.spanner.service.service.UserService;
import com.lujianfeng.spanner.vo.user.FriendRelationVO;
import com.lujianfeng.spanner.vo.user.FriendRequestActionResultVO;
import com.lujianfeng.spanner.vo.user.FriendRequestHistoryItemVO;
import com.lujianfeng.spanner.vo.user.PageResultVO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/friends")
public class FriendRelationController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final UserRelationRepository userRelationRepository;
    private final UserRelationService userRelationService;
    private final SimpUserRegistry simpUserRegistry;

    public FriendRelationController(UserService userService,
                                    UserRepository userRepository,
                                    UserRelationRepository userRelationRepository,
                                    UserRelationService userRelationService,
                                    SimpUserRegistry simpUserRegistry) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.userRelationRepository = userRelationRepository;
        this.userRelationService = userRelationService;
        this.simpUserRegistry = simpUserRegistry;
    }

    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> apply(@RequestBody FriendActionRequestDTO requestDTO) {
        return handleRelationAction(requestDTO, ActionType.APPLY);
    }

    @PostMapping("/accept")
    public ResponseEntity<Map<String, Object>> accept(@RequestBody FriendActionRequestDTO requestDTO) {
        return handleRelationAction(requestDTO, ActionType.ACCEPT);
    }

    @PostMapping("/reject")
    public ResponseEntity<Map<String, Object>> reject(@RequestBody FriendActionRequestDTO requestDTO) {
        return handleRelationAction(requestDTO, ActionType.REJECT);
    }

    @PostMapping("/cancel")
    public ResponseEntity<Map<String, Object>> cancel(@RequestBody FriendCancelRequestDTO requestDTO) {
        UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, "未登录", "UNAUTHORIZED"));
        }
        if (requestDTO == null || requestDTO.getRequestId() == null || requestDTO.getRequestId().isBlank()) {
            return ResponseEntity.badRequest().body(error(400, "requestId 不能为空", "FRIEND_REQUEST_INVALID_PARAM"));
        }

        try {
            FriendRequestActionResultVO result = userRelationService.cancelRelation(currentUser, requestDTO.getRequestId().trim());
            return ResponseEntity.ok(success("好友申请已取消", result));
        } catch (IllegalStateException e) {
            return toActionConflictOrNotFound(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error(500, "服务器内部错误", "INTERNAL_ERROR"));
        }
    }

    @DeleteMapping("/{friendAccount}")
    public ResponseEntity<Map<String, Object>> remove(@PathVariable String friendAccount) {
        FriendActionRequestDTO requestDTO = new FriendActionRequestDTO();
        requestDTO.setFriendAccount(friendAccount);
        return handleRelationAction(requestDTO, ActionType.REMOVE);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listFriends() {
        UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, "未登录"));
        }

        List<FriendRelationVO> data = userRelationService.listFriendRelations(currentUser).stream()
                .map(relation -> toFriendVOFromFriendSide(relation, currentUser))
                .toList();
        return ResponseEntity.ok(success("查询好友列表成功", data));
    }

    @GetMapping("/requests/pending")
    public ResponseEntity<Map<String, Object>> listPendingRequests() {
        UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, "未登录"));
        }

        List<FriendRelationVO> data = userRelationService.listPendingRequests(currentUser).stream()
                .map(this::toFriendVOFromRequesterSide)
                .toList();
        return ResponseEntity.ok(success("查询待处理好友申请成功", data));
    }

    @GetMapping("/requests/history")
    public ResponseEntity<Map<String, Object>> listHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String keyword
    ) {
        UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, "未登录", "UNAUTHORIZED"));
        }

        if (page == null || page < 1) {
            return ResponseEntity.badRequest().body(error(400, "page 必须从 1 开始", "FRIEND_REQUEST_INVALID_PARAM"));
        }
        if (size == null || size < 1 || size > 100) {
            return ResponseEntity.badRequest().body(error(400, "size 必须在 1 到 100 之间", "FRIEND_REQUEST_INVALID_PARAM"));
        }

        Set<String> statuses;
        try {
            statuses = parseStatuses(status);
            validateDirection(direction);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage(), "FRIEND_REQUEST_INVALID_PARAM"));
        }

        Instant parsedStartTime;
        Instant parsedEndTime;
        try {
            parsedStartTime = parseInstant(startTime);
            parsedEndTime = parseInstant(endTime);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(error(400, "时间格式非法，请使用 ISO-8601 UTC", "FRIEND_REQUEST_INVALID_PARAM"));
        }

        if (parsedStartTime != null && parsedEndTime != null && parsedStartTime.isAfter(parsedEndTime)) {
            return ResponseEntity.badRequest().body(error(400, "startTime 不能晚于 endTime", "FRIEND_REQUEST_INVALID_PARAM"));
        }

        PageResultVO<FriendRequestHistoryItemVO> result = userRelationService.queryRequestHistory(
                currentUser,
                page,
                size,
                direction,
                statuses,
                parsedStartTime,
                parsedEndTime,
                keyword
        );
        return ResponseEntity.ok(success("查询好友申请历史成功", result));
    }

    @GetMapping("/requests/history/{requestId}")
    public ResponseEntity<Map<String, Object>> historyDetail(@PathVariable String requestId) {
        UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, "未登录", "UNAUTHORIZED"));
        }
        if (requestId == null || requestId.isBlank()) {
            return ResponseEntity.badRequest().body(error(400, "requestId 不能为空", "FRIEND_REQUEST_INVALID_PARAM"));
        }

        try {
            FriendRequestHistoryItemVO result = userRelationService.queryRequestHistoryDetail(currentUser, requestId.trim());
            return ResponseEntity.ok(success("查询好友申请详情成功", result));
        } catch (IllegalStateException e) {
            return toActionConflictOrNotFound(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error(500, "服务器内部错误", "INTERNAL_ERROR"));
        }
    }

    @GetMapping("/users/{account}")
    public ResponseEntity<Map<String, Object>> getUserByAccount(@PathVariable String account) {
        UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, "未登录"));
        }
        if (account == null || account.isBlank()) {
            return ResponseEntity.badRequest().body(error(400, "account 不能为空"));
        }

        UserEntity target = userRepository.findByAccount(account.trim());
        if (target == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(404, "用户账号不存在"));
        }

        UserRelation relation = userRelationRepository.findByUserAndFriend(currentUser, target)
                .orElseGet(() -> userRelationRepository.findByUserAndFriend(target, currentUser).orElse(null));

        Map<String, Object> data = new HashMap<>();
        data.put("account", target.getAccount());
        data.put("realName", target.getRealName());
        data.put("avatarUrl", target.getAvatarUrl());
        data.put("signature", target.getSignature());
        data.put("isVip", isVip(target));
        data.put("growthValue", normalizeGrowthValue(target));
        data.put("vipLevel", normalizeVipLevel(target));
        data.put("isSelf", currentUser.getId().equals(target.getId()));
        data.put("relationType", relation == null ? null : relation.getRelationType());
        data.put("verificationMessage", relation == null ? null : relation.getVerificationMessage());

        return ResponseEntity.ok(success("查询用户成功", data));
    }

    private ResponseEntity<Map<String, Object>> handleRelationAction(FriendActionRequestDTO requestDTO, ActionType actionType) {
        UserEntity currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, "未登录"));
        }

        if (requestDTO == null || requestDTO.getFriendAccount() == null || requestDTO.getFriendAccount().isBlank()) {
            return ResponseEntity.badRequest().body(error(400, "friendAccount 不能为空"));
        }

        UserEntity friend = userRepository.findByAccount(requestDTO.getFriendAccount().trim());
        if (friend == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(404, "好友账号不存在"));
        }

        try {
            FriendRequestActionResultVO result = switch (actionType) {
                case APPLY -> userRelationService.applyRelation(currentUser, friend, requestDTO.getVerificationMessage());
                case ACCEPT -> userRelationService.acceptRelation(currentUser, friend);
                case REJECT -> userRelationService.rejectRelation(currentUser, friend);
                case REMOVE -> {
                    userRelationService.removeRelation(currentUser, friend);
                    yield null;
                }
            };
            return ResponseEntity.ok(success(actionType.successMessage, result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage()));
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("过于频繁")) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(error(429, e.getMessage(), "FRIEND_REQUEST_RATE_LIMITED"));
            }
            if (e.getMessage() != null && e.getMessage().contains("不存在")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(404, e.getMessage(), "FRIEND_REQUEST_NOT_FOUND"));
            }
            if (e.getMessage() != null && e.getMessage().contains("无权限")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(403, e.getMessage(), "FRIEND_REQUEST_FORBIDDEN"));
            }
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(error(409, e.getMessage(), "FRIEND_REQUEST_STATE_CONFLICT"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(500, "服务器内部错误"));
        }
    }

    private UserEntity getCurrentUser() {
        return userService.getCurrentUserEntity();
    }

    private Set<String> parseStatuses(String status) {
        if (status == null || status.isBlank()) {
            return Set.of();
        }

        Set<String> statuses = Arrays.stream(status.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());

        Set<String> allowed = Set.of("PENDING", "ACCEPTED", "REJECTED", "CANCELED", "EXPIRED");
        if (!allowed.containsAll(statuses)) {
            throw new IllegalArgumentException("status 参数非法");
        }
        return statuses;
    }

    private void validateDirection(String direction) {
        if (direction == null || direction.isBlank()) {
            return;
        }
        String normalized = direction.trim().toUpperCase(Locale.ROOT);
        if (!"INBOUND".equals(normalized) && !"OUTBOUND".equals(normalized)) {
            throw new IllegalArgumentException("direction 参数非法");
        }
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Instant.parse(value.trim());
    }

    private FriendRelationVO toFriendVOFromFriendSide(UserRelation relation, UserEntity currentUser) {
        UserEntity friend = relation.getUser().getId().equals(currentUser.getId())
                ? relation.getFriend()
                : relation.getUser();
        return FriendRelationVO.builder()
                .account(friend.getAccount())
                .realName(friend.getRealName())
                .avatarUrl(friend.getAvatarUrl())
                .online(isUserOnline(friend.getAccount()))
                .region(friend.getAddress())
                .email(friend.getEmail())
                .phone(friend.getPhone())
                .gender(friend.getGender())
                .signature(friend.getSignature())
                .age(friend.getAge())
                .isVip(isVip(friend))
                .growthValue(normalizeGrowthValue(friend))
                .vipLevel(normalizeVipLevel(friend))
                .relationType(relation.getRelationType())
                .verificationMessage(relation.getVerificationMessage())
                .createTime(relation.getCreateTime())
                .build();
    }

    private FriendRelationVO toFriendVOFromRequesterSide(UserRelation relation) {
        UserEntity requester = relation.getUser();
        return FriendRelationVO.builder()
                .account(requester.getAccount())
                .realName(requester.getRealName())
                .avatarUrl(requester.getAvatarUrl())
                .online(isUserOnline(requester.getAccount()))
                .region(requester.getAddress())
                .email(requester.getEmail())
                .phone(requester.getPhone())
                .gender(requester.getGender())
                .signature(requester.getSignature())
                .age(requester.getAge())
                .isVip(isVip(requester))
                .growthValue(normalizeGrowthValue(requester))
                .vipLevel(normalizeVipLevel(requester))
                .relationType(relation.getRelationType())
                .verificationMessage(relation.getVerificationMessage())
                .createTime(relation.getCreateTime())
                .build();
    }

    private boolean isVip(UserEntity user) {
        return user != null
                && user.getVipExpireAt() != null
                && user.getVipExpireAt().isAfter(LocalDateTime.now());
    }

    private long normalizeGrowthValue(UserEntity user) {
        if (user == null || user.getGrowthValue() == null) {
            return 0L;
        }
        return Math.max(user.getGrowthValue(), 0L);
    }

    private int normalizeVipLevel(UserEntity user) {
        if (user == null || user.getUserLevel() == null || user.getUserLevel() < 1) {
            return 1;
        }
        return user.getUserLevel();
    }

    private boolean isUserOnline(String account) {
        SimpUser user = simpUserRegistry.getUser(account);
        return user != null && !user.getSessions().isEmpty();
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

    private Map<String, Object> error(int code, String message, String businessCode) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("status", "error");
        body.put("message", message);
        body.put("errorCode", businessCode);
        return body;
    }

    private ResponseEntity<Map<String, Object>> toActionConflictOrNotFound(String message) {
        if (message != null && message.contains("不存在")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(error(404, message, "FRIEND_REQUEST_NOT_FOUND"));
        }
        if (message != null && message.contains("无权限")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(error(403, message, "FRIEND_REQUEST_FORBIDDEN"));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(409, message, "FRIEND_REQUEST_STATE_CONFLICT"));
    }

    private enum ActionType {
        APPLY("好友申请已发送"),
        ACCEPT("好友申请已同意"),
        REJECT("好友申请已拒绝"),
        REMOVE("好友关系已删除");

        private final String successMessage;

        ActionType(String successMessage) {
            this.successMessage = successMessage;
        }
    }
}
