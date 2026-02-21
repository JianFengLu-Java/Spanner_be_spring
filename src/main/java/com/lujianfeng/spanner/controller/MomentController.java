package com.lujianfeng.spanner.controller;

import com.lujianfeng.spanner.dto.moment.MomentCommentCreateRequestDTO;
import com.lujianfeng.spanner.dto.moment.MomentCreateRequestDTO;
import com.lujianfeng.spanner.service.service.MomentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/moments")
public class MomentController {

    private static final Logger log = LoggerFactory.getLogger(MomentController.class);
    private final MomentService momentService;

    public MomentController(MomentService momentService) {
        this.momentService = momentService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listMoments(
            @RequestParam(required = false, defaultValue = "recommend") String tab,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng
    ) {
        try {
            return ResponseEntity.ok(success("查询动态流成功", momentService.listMoments(tab, keyword, cursor, size, lat, lng)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage(), "MOMENT_INVALID_PARAM"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, e.getMessage(), "UNAUTHORIZED"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(500, "服务器内部错误", "INTERNAL_ERROR"));
        }
    }

    @GetMapping("/about-me")
    public ResponseEntity<Map<String, Object>> listAboutMe(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "20") Integer size
    ) {
        log.info("进入关于我的动态接口, cursor={}, size={}", cursor, size);
        try {
            return ResponseEntity.ok(success("查询关于我的动态成功", momentService.listAboutMe(cursor, size)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage(), "MOMENT_INVALID_PARAM"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, e.getMessage(), "UNAUTHORIZED"));
        } catch (Exception e) {
            log.error("查询关于我的动态失败, cursor={}, size={}", cursor, size, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(500, "服务器内部错误", "INTERNAL_ERROR"));
        }
    }

    @GetMapping("/{momentId}")
    public ResponseEntity<Map<String, Object>> getMomentDetail(@PathVariable String momentId) {
        try {
            return ResponseEntity.ok(success("查询动态详情成功", momentService.getMomentDetail(momentId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage(), "MOMENT_INVALID_PARAM"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(404, e.getMessage(), "MOMENT_NOT_FOUND"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, e.getMessage(), "UNAUTHORIZED"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(500, "服务器内部错误", "INTERNAL_ERROR"));
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createMoment(@RequestBody MomentCreateRequestDTO request) {
        try {
            return ResponseEntity.ok(success("发布动态成功", momentService.createMoment(request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage(), "MOMENT_INVALID_PARAM"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, e.getMessage(), "UNAUTHORIZED"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(500, "服务器内部错误", "INTERNAL_ERROR"));
        }
    }

    @PutMapping("/{momentId}")
    public ResponseEntity<Map<String, Object>> updateMoment(@PathVariable String momentId,
                                                            @RequestBody MomentCreateRequestDTO request) {
        try {
            return ResponseEntity.ok(success("更新动态成功", momentService.updateMoment(momentId, request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage(), "MOMENT_INVALID_PARAM"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(404, e.getMessage(), "MOMENT_NOT_FOUND"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(403, e.getMessage(), "MOMENT_FORBIDDEN"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, e.getMessage(), "UNAUTHORIZED"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(500, "服务器内部错误", "INTERNAL_ERROR"));
        }
    }

    @DeleteMapping("/{momentId}")
    public ResponseEntity<Map<String, Object>> deleteMoment(@PathVariable String momentId) {
        try {
            momentService.deleteMoment(momentId);
            return ResponseEntity.ok(success("删除动态成功", Map.of()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage(), "MOMENT_INVALID_PARAM"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(404, e.getMessage(), "MOMENT_NOT_FOUND"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(403, e.getMessage(), "MOMENT_FORBIDDEN"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, e.getMessage(), "UNAUTHORIZED"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(500, "服务器内部错误", "INTERNAL_ERROR"));
        }
    }

    @PostMapping("/{momentId}/likes")
    public ResponseEntity<Map<String, Object>> like(@PathVariable String momentId) {
        try {
            return ResponseEntity.ok(success("点赞成功", momentService.likeMoment(momentId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage(), "MOMENT_INVALID_PARAM"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(404, e.getMessage(), "MOMENT_NOT_FOUND"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, e.getMessage(), "UNAUTHORIZED"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(500, "服务器内部错误", "INTERNAL_ERROR"));
        }
    }

    @DeleteMapping("/{momentId}/likes")
    public ResponseEntity<Map<String, Object>> unlike(@PathVariable String momentId) {
        try {
            return ResponseEntity.ok(success("取消点赞成功", momentService.unlikeMoment(momentId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage(), "MOMENT_INVALID_PARAM"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(404, e.getMessage(), "MOMENT_NOT_FOUND"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, e.getMessage(), "UNAUTHORIZED"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(500, "服务器内部错误", "INTERNAL_ERROR"));
        }
    }

    @GetMapping("/{momentId}/comments")
    public ResponseEntity<Map<String, Object>> listComments(
            @PathVariable String momentId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @RequestParam(required = false) String parentCommentId,
            @RequestParam(required = false, defaultValue = "latest") String sort
    ) {
        try {
            return ResponseEntity.ok(success("查询评论成功", momentService.listComments(momentId, cursor, size, parentCommentId, sort)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage(), "MOMENT_INVALID_PARAM"));
        } catch (IllegalStateException e) {
            String code = e.getMessage() != null && e.getMessage().contains("评论") ? "MOMENT_COMMENT_NOT_FOUND" : "MOMENT_NOT_FOUND";
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(404, e.getMessage(), code));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, e.getMessage(), "UNAUTHORIZED"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(500, "服务器内部错误", "INTERNAL_ERROR"));
        }
    }

    @PostMapping("/{momentId}/comments")
    public ResponseEntity<Map<String, Object>> createComment(
            @PathVariable String momentId,
            @RequestBody MomentCommentCreateRequestDTO request
    ) {
        try {
            return ResponseEntity.ok(success("发表评论成功", momentService.createComment(momentId, request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage(), "MOMENT_INVALID_PARAM"));
        } catch (IllegalStateException e) {
            String code = e.getMessage() != null && e.getMessage().contains("评论") ? "MOMENT_COMMENT_NOT_FOUND" : "MOMENT_NOT_FOUND";
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(404, e.getMessage(), code));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, e.getMessage(), "UNAUTHORIZED"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(500, "服务器内部错误", "INTERNAL_ERROR"));
        }
    }

    @GetMapping("/{momentId}/likes")
    public ResponseEntity<Map<String, Object>> listLikes(
            @PathVariable String momentId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "20") Integer size
    ) {
        try {
            return ResponseEntity.ok(success("查询点赞信息成功", momentService.listLikes(momentId, cursor, size)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage(), "MOMENT_INVALID_PARAM"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(404, e.getMessage(), "MOMENT_NOT_FOUND"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, e.getMessage(), "UNAUTHORIZED"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(500, "服务器内部错误", "INTERNAL_ERROR"));
        }
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
}
