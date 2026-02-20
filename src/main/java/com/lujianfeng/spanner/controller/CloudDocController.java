package com.lujianfeng.spanner.controller;

import com.lujianfeng.spanner.dto.cloud.CloudDocCreateRequestDTO;
import com.lujianfeng.spanner.dto.cloud.CloudDocSaveRequestDTO;
import com.lujianfeng.spanner.dto.cloud.CloudDocShareCreateRequestDTO;
import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.service.CloudDocService;
import com.lujianfeng.spanner.service.CloudDocVersionConflictException;
import com.lujianfeng.spanner.service.service.UserService;
import com.lujianfeng.spanner.vo.cloud.CloudDocVersionConflictVO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/cloud-docs")
public class CloudDocController {

    private final CloudDocService cloudDocService;
    private final UserService userService;

    public CloudDocController(CloudDocService cloudDocService, UserService userService) {
        this.cloudDocService = cloudDocService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(@RequestParam(required = false) Integer page,
                                                    @RequestParam(required = false) Integer size,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String sort) {
        UserEntity currentUser = userService.getCurrentUserEntity();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            return ResponseEntity.ok(success("查询云文档列表成功", cloudDocService.listMyDocs(currentUser, page, size, keyword, sort)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage(), "CLOUD_DOC_INVALID_PARAM"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(500, "服务异常", "INTERNAL_ERROR"));
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody(required = false) CloudDocCreateRequestDTO requestDTO) {
        UserEntity currentUser = userService.getCurrentUserEntity();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            String title = requestDTO == null ? null : requestDTO.getTitle();
            return ResponseEntity.ok(success("创建云文档成功", cloudDocService.createDoc(currentUser, title)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage(), "CLOUD_DOC_INVALID_PARAM"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(500, "服务异常", "INTERNAL_ERROR"));
        }
    }

    @GetMapping("/{docId}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable String docId) {
        UserEntity currentUser = userService.getCurrentUserEntity();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            return ResponseEntity.ok(success("查询云文档详情成功", cloudDocService.getDocDetail(currentUser, docId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage(), "CLOUD_DOC_INVALID_PARAM"));
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("无权限")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(403, e.getMessage(), "CLOUD_DOC_FORBIDDEN"));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(404, e.getMessage(), "CLOUD_DOC_NOT_FOUND"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(500, "服务异常", "INTERNAL_ERROR"));
        }
    }

    @PutMapping("/{docId}")
    public ResponseEntity<Map<String, Object>> save(@PathVariable String docId,
                                                     @RequestBody CloudDocSaveRequestDTO requestDTO) {
        UserEntity currentUser = userService.getCurrentUserEntity();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            return ResponseEntity.ok(success("保存云文档成功", cloudDocService.saveDoc(currentUser, docId, requestDTO)));
        } catch (CloudDocVersionConflictException e) {
            CloudDocVersionConflictVO conflict = CloudDocVersionConflictVO.builder()
                    .latestVersion(e.getLatestVersion())
                    .latestUpdatedAt(formatUtc(e.getLatestUpdatedAt()))
                    .build();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorWithData(409,
                    "版本冲突",
                    "CLOUD_DOC_VERSION_CONFLICT",
                    conflict));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage(), "CLOUD_DOC_INVALID_PARAM"));
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("无权限")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(403, e.getMessage(), "CLOUD_DOC_FORBIDDEN"));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(404, e.getMessage(), "CLOUD_DOC_NOT_FOUND"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(500, "服务异常", "INTERNAL_ERROR"));
        }
    }

    @DeleteMapping("/{docId}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String docId) {
        UserEntity currentUser = userService.getCurrentUserEntity();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            cloudDocService.deleteDoc(currentUser, docId);
            return ResponseEntity.ok(success("删除云文档成功", Map.of()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage(), "CLOUD_DOC_INVALID_PARAM"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(500, "服务异常", "INTERNAL_ERROR"));
        }
    }

    @PostMapping("/{docId}/share")
    public ResponseEntity<Map<String, Object>> share(@PathVariable String docId,
                                                     @RequestBody CloudDocShareCreateRequestDTO requestDTO) {
        UserEntity currentUser = userService.getCurrentUserEntity();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            return ResponseEntity.ok(success("分享云文档成功", cloudDocService.shareDocToFriend(currentUser, docId, requestDTO)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage(), "CLOUD_DOC_INVALID_PARAM"));
        } catch (IllegalStateException e) {
            return shareStateError(e);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(500, "服务异常", "INTERNAL_ERROR"));
        }
    }

    @GetMapping("/shares/{shareNo}")
    public ResponseEntity<Map<String, Object>> viewSharedDoc(@PathVariable String shareNo) {
        UserEntity currentUser = userService.getCurrentUserEntity();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            return ResponseEntity.ok(success("查询分享文档成功", cloudDocService.getSharedDoc(currentUser, shareNo)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage(), "CLOUD_DOC_INVALID_PARAM"));
        } catch (IllegalStateException e) {
            return shareStateError(e);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(500, "服务异常", "INTERNAL_ERROR"));
        }
    }

    @DeleteMapping("/shares/{shareNo}")
    public ResponseEntity<Map<String, Object>> revokeShare(@PathVariable String shareNo) {
        UserEntity currentUser = userService.getCurrentUserEntity();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            return ResponseEntity.ok(success("撤销分享成功", cloudDocService.revokeShare(currentUser, shareNo)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage(), "CLOUD_DOC_INVALID_PARAM"));
        } catch (IllegalStateException e) {
            return shareStateError(e);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(500, "服务异常", "INTERNAL_ERROR"));
        }
    }

    @GetMapping("/shares/received")
    public ResponseEntity<Map<String, Object>> listReceivedShares(@RequestParam(required = false) Integer page,
                                                                  @RequestParam(required = false) Integer size,
                                                                  @RequestParam(required = false) String status) {
        UserEntity currentUser = userService.getCurrentUserEntity();
        if (currentUser == null) {
            return unauthorized();
        }
        try {
            return ResponseEntity.ok(success("查询我收到的分享成功", cloudDocService.listReceivedShares(currentUser, page, size, status)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage(), "CLOUD_DOC_INVALID_PARAM"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(500, "服务异常", "INTERNAL_ERROR"));
        }
    }

    private ResponseEntity<Map<String, Object>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, "未登录", "UNAUTHORIZED"));
    }

    private Map<String, Object> success(String message, Object data) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", 200);
        body.put("status", "success");
        body.put("message", message);
        body.put("data", data == null ? Map.of() : data);
        return body;
    }

    private Map<String, Object> error(int code, String message, String errorCode) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("status", "error");
        body.put("message", message);
        body.put("errorCode", errorCode);
        return body;
    }

    private Map<String, Object> errorWithData(int code, String message, String errorCode, Object data) {
        Map<String, Object> body = error(code, message, errorCode);
        body.put("data", data == null ? Map.of() : data);
        return body;
    }

    private String formatUtc(java.time.LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC)
                .toInstant()
                .toString();
    }

    private ResponseEntity<Map<String, Object>> shareStateError(IllegalStateException e) {
        String message = e.getMessage() == null ? "操作失败" : e.getMessage();
        if (message.contains("不存在")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(404, message, "CLOUD_DOC_NOT_FOUND"));
        }
        if (message.contains("无权限")
                || message.contains("仅支持分享给好友")
                || message.contains("已失效")
                || message.contains("已过期")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(403, message, "CLOUD_DOC_FORBIDDEN"));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error(409, message, "CLOUD_DOC_VERSION_CONFLICT"));
    }
}
