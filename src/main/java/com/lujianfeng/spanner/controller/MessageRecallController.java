package com.lujianfeng.spanner.controller;

import com.lujianfeng.spanner.dto.message.MessageRecallRequestDTO;
import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.service.MessageRecallService;
import com.lujianfeng.spanner.service.service.UserService;
import com.lujianfeng.spanner.vo.message.MessageRecallResultVO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/messages")
public class MessageRecallController {

    private final UserService userService;
    private final MessageRecallService messageRecallService;

    public MessageRecallController(UserService userService, MessageRecallService messageRecallService) {
        this.userService = userService;
        this.messageRecallService = messageRecallService;
    }

    @PostMapping("/{messageId}/recall")
    public ResponseEntity<Map<String, Object>> recall(@PathVariable String messageId,
                                                      @RequestBody(required = false) MessageRecallRequestDTO request) {
        UserEntity currentUser = userService.getCurrentUserEntity();
        if (currentUser == null || currentUser.getAccount() == null || currentUser.getAccount().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(error(401, "UNAUTHORIZED", "未登录"));
        }
        try {
            MessageRecallResultVO result = messageRecallService.recall(currentUser.getAccount(), messageId, request);
            return ResponseEntity.ok(success("撤回成功", result));
        } catch (IllegalArgumentException e) {
            String message = e.getMessage() == null ? "参数非法" : e.getMessage();
            if (message.contains("不存在")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(error(404, "MESSAGE_NOT_FOUND", message));
            }
            return ResponseEntity.badRequest()
                    .body(error(400, "RECALL_INVALID_PARAM", message));
        } catch (IllegalStateException e) {
            String message = e.getMessage() == null ? "状态冲突" : e.getMessage();
            if (message.contains("仅支持撤回自己")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(error(403, "MESSAGE_RECALL_FORBIDDEN", message));
            }
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(error(409, "MESSAGE_RECALL_CONFLICT", message));
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
