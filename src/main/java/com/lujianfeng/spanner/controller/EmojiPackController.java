package com.lujianfeng.spanner.controller;

import com.lujianfeng.spanner.service.service.EmojiPackService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/emojis")
public class EmojiPackController {
    private final EmojiPackService emojiPackService;

    public EmojiPackController(EmojiPackService emojiPackService) {
        this.emojiPackService = emojiPackService;
    }

    @PostMapping("/gallery/upload")
    public ResponseEntity<Map<String, Object>> uploadToGallery(@RequestParam("file") MultipartFile file,
                                                               @RequestParam(required = false) String displayName) {
        try {
            return ResponseEntity.ok(success("上传表情到图库成功", emojiPackService.uploadToGallery(file, displayName)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage(), "EMOJI_INVALID_PARAM"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, e.getMessage(), "UNAUTHORIZED"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(500, e.getMessage(), "EMOJI_UPLOAD_FAILED"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(500, "服务器内部错误", "INTERNAL_ERROR"));
        }
    }

    @GetMapping("/gallery/my")
    public ResponseEntity<Map<String, Object>> listMyGallery(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword
    ) {
        try {
            return ResponseEntity.ok(success("查询我的图库成功", emojiPackService.listMyGallery(page, size, keyword)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage(), "EMOJI_INVALID_PARAM"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, e.getMessage(), "UNAUTHORIZED"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(500, "服务器内部错误", "INTERNAL_ERROR"));
        }
    }

    @DeleteMapping("/gallery/{emojiId}")
    public ResponseEntity<Map<String, Object>> deleteFromGallery(@PathVariable String emojiId) {
        try {
            emojiPackService.deleteFromGallery(emojiId);
            return ResponseEntity.ok(success("删除图库表情成功", Map.of()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage(), "EMOJI_INVALID_PARAM"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(404, e.getMessage(), "EMOJI_NOT_FOUND"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, e.getMessage(), "UNAUTHORIZED"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(500, "服务器内部错误", "INTERNAL_ERROR"));
        }
    }

    @PostMapping("/favorites/{emojiId}")
    public ResponseEntity<Map<String, Object>> favoriteEmoji(@PathVariable String emojiId) {
        try {
            return ResponseEntity.ok(success("收藏表情成功", emojiPackService.favoriteEmoji(emojiId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage(), "EMOJI_INVALID_PARAM"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(404, e.getMessage(), "EMOJI_NOT_FOUND"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, e.getMessage(), "UNAUTHORIZED"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(500, "服务器内部错误", "INTERNAL_ERROR"));
        }
    }

    @DeleteMapping("/favorites/{emojiId}")
    public ResponseEntity<Map<String, Object>> unfavoriteEmoji(@PathVariable String emojiId) {
        try {
            emojiPackService.unfavoriteEmoji(emojiId);
            return ResponseEntity.ok(success("取消收藏成功", Map.of()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage(), "EMOJI_INVALID_PARAM"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(404, e.getMessage(), "EMOJI_NOT_FOUND"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(401, e.getMessage(), "UNAUTHORIZED"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(500, "服务器内部错误", "INTERNAL_ERROR"));
        }
    }

    @GetMapping("/favorites")
    public ResponseEntity<Map<String, Object>> listMyFavorites(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        try {
            return ResponseEntity.ok(success("查询收藏表情成功", emojiPackService.listMyFavorites(page, size)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(400, e.getMessage(), "EMOJI_INVALID_PARAM"));
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
