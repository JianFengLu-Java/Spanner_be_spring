package com.lujianfeng.spanner.controller;

import com.lujianfeng.spanner.dto.message.MessageReactionToggleRequestDTO;
import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.service.MessageReactionService;
import com.lujianfeng.spanner.service.service.UserService;
import com.lujianfeng.spanner.vo.message.MessageReactionSnapshotVO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/messages")
public class MessageReactionController {

    private final UserService userService;
    private final MessageReactionService messageReactionService;

    public MessageReactionController(UserService userService, MessageReactionService messageReactionService) {
        this.userService = userService;
        this.messageReactionService = messageReactionService;
    }

    @PutMapping("/{messageId}/reactions/toggle")
    public ResponseEntity<Map<String, Object>> toggleReaction(@PathVariable String messageId,
                                                              @RequestBody(required = false) MessageReactionToggleRequestDTO request) {
        UserEntity currentUser = userService.getCurrentUserEntity();
        if (currentUser == null || currentUser.getAccount() == null || currentUser.getAccount().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(error(401, "UNAUTHORIZED", "未登录"));
        }
        try {
            MessageReactionSnapshotVO snapshot =
                    messageReactionService.toggleReaction(currentUser.getAccount(), messageId, request);
            return ResponseEntity.ok(success("success", snapshot));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, "REACTION_INVALID_PARAM", e.getMessage()));
        } catch (IllegalStateException e) {
            String message = e.getMessage() == null ? "消息状态冲突" : e.getMessage();
            if (message.contains("无权")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(403, "MESSAGE_FORBIDDEN", message));
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error(409, "REACTION_STATE_CONFLICT", message));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error(500, "INTERNAL_ERROR", "服务端异常"));
        }
    }

    @GetMapping("/{messageId}/reactions")
    public ResponseEntity<Map<String, Object>> getReactions(@PathVariable String messageId,
                                                            @RequestParam Long chatId,
                                                            @RequestParam(required = false) String serverMessageId,
                                                            @RequestParam(required = false) String clientMessageId) {
        UserEntity currentUser = userService.getCurrentUserEntity();
        if (currentUser == null || currentUser.getAccount() == null || currentUser.getAccount().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(error(401, "UNAUTHORIZED", "未登录"));
        }
        try {
            MessageReactionSnapshotVO snapshot = messageReactionService.getSnapshot(
                    currentUser.getAccount(), messageId, chatId, serverMessageId, clientMessageId);
            return ResponseEntity.ok(success("success", snapshot));
        } catch (IllegalArgumentException e) {
            String message = e.getMessage() == null ? "参数非法" : e.getMessage();
            if (message.contains("不存在")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(404, "MESSAGE_NOT_FOUND", message));
            }
            return ResponseEntity.badRequest().body(error(400, "REACTION_INVALID_PARAM", message));
        } catch (IllegalStateException e) {
            String message = e.getMessage() == null ? "消息状态冲突" : e.getMessage();
            if (message.contains("无权")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(403, "MESSAGE_FORBIDDEN", message));
            }
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error(409, "REACTION_STATE_CONFLICT", message));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error(500, "INTERNAL_ERROR", "服务端异常"));
        }
    }

    private Map<String, Object> success(String message, Object data) {
        return Map.of(
                "code", 200,
                "status", "OK",
                "message", message,
                "data", data == null ? Map.of() : data
        );
    }

    private Map<String, Object> error(int code, String status, String message) {
        return Map.of(
                "code", code,
                "status", status,
                "message", message
        );
    }
}
