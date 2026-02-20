package com.lujianfeng.spanner.controller;

import com.lujianfeng.spanner.service.service.FileService;
import com.lujianfeng.spanner.vo.file.FileObjectVO;
import com.lujianfeng.spanner.vo.file.FileUploadResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author Lujianfeng
 * @version 1.0
 * @date 2025/12/15
 * @since 1.0
 */

@RestController
@RequestMapping("/files")
public class FileController {


    private final FileService fileService;
    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;

    public FileController(@Autowired FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * 前端上传文件
     *
     * @param file 参数名为fileData
     * @return JSON对象
     */
    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updata(@RequestParam("fileData") MultipartFile file) {
        try {
            String fileUrl = fileService.upload(file);
            return ResponseEntity.ok(
                    Map.of(
                            "status", "success",
                            "fileUrl", fileUrl
                    )
            );
        } catch (Exception e) {


        }
        return ResponseEntity.ok(
                Map.of(
                        "message", "error"
                )
        );
    }

    @PostMapping("/update/avatar")
    public ResponseEntity<Map<String, Object>> updateAvatar(@RequestParam("file") MultipartFile file) {
        try {
            String fileUrl = fileService.upload(file);
            return ResponseEntity.ok(
                    Map.of(
                            "status", "success",
                            "fileUrl", fileUrl
                    )
            );
        } catch (Exception e) {

        }
        return ResponseEntity.ok(
                Map.of(
                        "message", "error"
                )
        );
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        try {
            ValidationResult validationResult = validateImage(file);
            if (validationResult != null) {
                return ResponseEntity.status(validationResult.status).body(Map.of(
                        "code", validationResult.code,
                        "status", "error",
                        "message", validationResult.message,
                        "errorCode", validationResult.errorCode
                ));
            }

            FileUploadResultVO uploadResult = fileService.uploadImage(file);
            if (uploadResult == null || uploadResult.getObjectName() == null || uploadResult.getObjectName().isBlank()) {
                return ResponseEntity.internalServerError().body(Map.of(
                        "code", 500,
                        "status", "error",
                        "message", "上传失败",
                        "errorCode", "INTERNAL_ERROR"
                ));
            }

            Integer width = null;
            Integer height = null;
            try {
                BufferedImage image = ImageIO.read(file.getInputStream());
                if (image != null) {
                    width = image.getWidth();
                    height = image.getHeight();
                }
            } catch (IOException ignored) {
            }

            Map<String, Object> data = new HashMap<>();
            String imageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/files/image/")
                    .path(uploadResult.getObjectName())
                    .toUriString();
            data.put("url", imageUrl);
            data.put("objectName", uploadResult.getObjectName());
            data.put("width", width);
            data.put("height", height);
            data.put("size", file.getSize());

            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "status", "success",
                    "message", "上传成功",
                    "data", data
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "code", 500,
                    "status", "error",
                    "message", "服务器内部错误",
                    "errorCode", "INTERNAL_ERROR"
            ));
        }
    }

    @GetMapping("/image/{objectName:.+}")
    public ResponseEntity<byte[]> getImage(@PathVariable String objectName) {
        if (objectName == null || objectName.isBlank()) {
            return ResponseEntity.badRequest().body(new byte[0]);
        }

        try {
            FileObjectVO image = fileService.getImage(objectName.trim());
            MediaType mediaType = MediaType.parseMediaType(image.getContentType());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, mediaType.toString())
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                    .contentLength(image.getSize() == null ? image.getData().length : image.getSize())
                    .body(image.getData());
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private ValidationResult validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return new ValidationResult(400, "file 不能为空", "CLOUD_DOC_INVALID_PARAM", org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            return new ValidationResult(413, "单张图片大小不能超过 10MB", "FILE_TOO_LARGE", org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE);
        }
        String contentType = file.getContentType();
        String normalizedType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (!normalizedType.startsWith("image/")) {
            return new ValidationResult(415, "仅支持图片文件上传", "FILE_TYPE_NOT_ALLOWED", org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
        if (!"image/jpeg".equals(normalizedType)
                && !"image/jpg".equals(normalizedType)
                && !"image/png".equals(normalizedType)
                && !"image/webp".equals(normalizedType)
                && !"image/gif".equals(normalizedType)) {
            return new ValidationResult(415, "仅支持 jpg/png/webp/gif", "FILE_TYPE_NOT_ALLOWED", org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
        return null;
    }

    private static class ValidationResult {
        private final int code;
        private final String message;
        private final String errorCode;
        private final org.springframework.http.HttpStatus status;

        private ValidationResult(int code, String message, String errorCode, org.springframework.http.HttpStatus status) {
            this.code = code;
            this.message = message;
            this.errorCode = errorCode;
            this.status = status;
        }
    }
}
