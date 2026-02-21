package com.lujianfeng.spanner.service.impl;

import com.lujianfeng.spanner.entity.emoji.EmojiFavoriteEntity;
import com.lujianfeng.spanner.entity.emoji.EmojiPackEntity;
import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.repository.EmojiFavoriteRepository;
import com.lujianfeng.spanner.repository.EmojiPackRepository;
import com.lujianfeng.spanner.service.service.EmojiPackService;
import com.lujianfeng.spanner.service.service.FileService;
import com.lujianfeng.spanner.service.service.UserService;
import com.lujianfeng.spanner.vo.emoji.EmojiPackItemVO;
import com.lujianfeng.spanner.vo.file.FileUploadResultVO;
import com.lujianfeng.spanner.vo.user.PageResultVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class EmojiPackServiceImpl implements EmojiPackService {
    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;
    private final UserService userService;
    private final FileService fileService;
    private final EmojiPackRepository emojiPackRepository;
    private final EmojiFavoriteRepository emojiFavoriteRepository;

    public EmojiPackServiceImpl(UserService userService,
                                FileService fileService,
                                EmojiPackRepository emojiPackRepository,
                                EmojiFavoriteRepository emojiFavoriteRepository) {
        this.userService = userService;
        this.fileService = fileService;
        this.emojiPackRepository = emojiPackRepository;
        this.emojiFavoriteRepository = emojiFavoriteRepository;
    }

    @Override
    @Transactional
    public EmojiPackItemVO uploadToGallery(MultipartFile file, String displayName) {
        UserEntity currentUser = requireCurrentUser();
        validateImage(file);

        FileUploadResultVO uploadResult = fileService.uploadImage(file);
        if (uploadResult == null || uploadResult.getObjectName() == null || uploadResult.getObjectName().isBlank()) {
            throw new IllegalStateException("上传失败");
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

        EmojiPackEntity entity = new EmojiPackEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setOwner(currentUser);
        entity.setDisplayName(normalizeDisplayName(displayName));
        entity.setObjectName(uploadResult.getObjectName());
        entity.setImageUrl(uploadResult.getUrl());
        entity.setContentType(uploadResult.getContentType());
        entity.setFileSize(uploadResult.getSize());
        entity.setWidth(width);
        entity.setHeight(height);
        entity.setFavoriteCount(0L);

        EmojiPackEntity saved = emojiPackRepository.save(entity);
        return toItemVO(saved, false);
    }

    @Override
    public PageResultVO<EmojiPackItemVO> listMyGallery(Integer page, Integer size, String keyword) {
        UserEntity currentUser = requireCurrentUser();
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        PageRequest pageRequest = PageRequest.of(normalizedPage - 1, normalizedSize);

        Page<EmojiPackEntity> dataPage;
        if (keyword == null || keyword.isBlank()) {
            dataPage = emojiPackRepository.findByOwnerOrderByCreatedAtDesc(currentUser, pageRequest);
        } else {
            dataPage = emojiPackRepository.findByOwnerAndDisplayNameContainingIgnoreCaseOrderByCreatedAtDesc(
                    currentUser,
                    keyword.trim(),
                    pageRequest
            );
        }

        List<EmojiPackItemVO> records = dataPage.getContent()
                .stream()
                .map(item -> toItemVO(item, emojiFavoriteRepository.existsByEmojiAndUser(item, currentUser)))
                .toList();

        return buildPageResult(records, normalizedPage, normalizedSize, dataPage.getTotalElements(), dataPage.getTotalPages());
    }

    @Override
    @Transactional
    public void deleteFromGallery(String emojiId) {
        UserEntity currentUser = requireCurrentUser();
        String normalizedEmojiId = normalizeEmojiId(emojiId);
        EmojiPackEntity emoji = emojiPackRepository.findByIdAndOwner(normalizedEmojiId, currentUser)
                .orElseThrow(() -> new IllegalStateException("表情不存在或无删除权限"));

        emojiFavoriteRepository.deleteByEmoji(emoji);
        emojiPackRepository.delete(emoji);
    }

    @Override
    @Transactional
    public EmojiPackItemVO favoriteEmoji(String emojiId) {
        UserEntity currentUser = requireCurrentUser();
        String normalizedEmojiId = normalizeEmojiId(emojiId);
        EmojiPackEntity emoji = emojiPackRepository.findById(normalizedEmojiId)
                .orElseThrow(() -> new IllegalStateException("表情不存在"));

        if (!emojiFavoriteRepository.existsByEmojiAndUser(emoji, currentUser)) {
            EmojiFavoriteEntity favorite = new EmojiFavoriteEntity();
            favorite.setEmoji(emoji);
            favorite.setUser(currentUser);
            emojiFavoriteRepository.save(favorite);
            emoji.setFavoriteCount(emoji.getFavoriteCount() + 1L);
            emojiPackRepository.save(emoji);
        }

        return toItemVO(emoji, true);
    }

    @Override
    @Transactional
    public void unfavoriteEmoji(String emojiId) {
        UserEntity currentUser = requireCurrentUser();
        String normalizedEmojiId = normalizeEmojiId(emojiId);
        EmojiPackEntity emoji = emojiPackRepository.findById(normalizedEmojiId)
                .orElseThrow(() -> new IllegalStateException("表情不存在"));

        emojiFavoriteRepository.findByEmojiAndUser(emoji, currentUser).ifPresent(favorite -> {
            emojiFavoriteRepository.delete(favorite);
            long newCount = Math.max(0L, emoji.getFavoriteCount() - 1L);
            emoji.setFavoriteCount(newCount);
            emojiPackRepository.save(emoji);
        });
    }

    @Override
    public PageResultVO<EmojiPackItemVO> listMyFavorites(Integer page, Integer size) {
        UserEntity currentUser = requireCurrentUser();
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        PageRequest pageRequest = PageRequest.of(normalizedPage - 1, normalizedSize);

        Page<EmojiFavoriteEntity> dataPage = emojiFavoriteRepository.findByUserOrderByCreatedAtDesc(currentUser, pageRequest);
        List<EmojiPackItemVO> records = dataPage.getContent()
                .stream()
                .map(EmojiFavoriteEntity::getEmoji)
                .map(emoji -> toItemVO(emoji, true))
                .toList();

        return buildPageResult(records, normalizedPage, normalizedSize, dataPage.getTotalElements(), dataPage.getTotalPages());
    }

    private UserEntity requireCurrentUser() {
        UserEntity currentUser = userService.getCurrentUserEntity();
        if (currentUser == null) {
            throw new SecurityException("未登录");
        }
        return currentUser;
    }

    private String normalizeEmojiId(String emojiId) {
        if (emojiId == null || emojiId.isBlank()) {
            throw new IllegalArgumentException("emojiId 不能为空");
        }
        return emojiId.trim();
    }

    private String normalizeDisplayName(String displayName) {
        if (displayName == null) {
            return null;
        }
        String normalized = displayName.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > 80) {
            throw new IllegalArgumentException("displayName 长度不能超过 80");
        }
        return normalized;
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 1) {
            throw new IllegalArgumentException("page 必须从 1 开始");
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1 || size > 100) {
            throw new IllegalArgumentException("size 必须在 1 到 100 之间");
        }
        return size;
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file 不能为空");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("单张图片大小不能超过 10MB");
        }
        String contentType = file.getContentType();
        String normalizedType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (!normalizedType.startsWith("image/")) {
            throw new IllegalArgumentException("仅支持图片文件上传");
        }
        if (!"image/jpeg".equals(normalizedType)
                && !"image/jpg".equals(normalizedType)
                && !"image/png".equals(normalizedType)
                && !"image/webp".equals(normalizedType)
                && !"image/gif".equals(normalizedType)) {
            throw new IllegalArgumentException("仅支持 jpg/png/webp/gif");
        }
    }

    private EmojiPackItemVO toItemVO(EmojiPackEntity entity, boolean favorited) {
        return EmojiPackItemVO.builder()
                .id(entity.getId())
                .displayName(entity.getDisplayName())
                .imageUrl(entity.getImageUrl())
                .objectName(entity.getObjectName())
                .contentType(entity.getContentType())
                .fileSize(entity.getFileSize())
                .width(entity.getWidth())
                .height(entity.getHeight())
                .favoriteCount(entity.getFavoriteCount())
                .favorited(favorited)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private PageResultVO<EmojiPackItemVO> buildPageResult(List<EmojiPackItemVO> records,
                                                          int page,
                                                          int size,
                                                          long total,
                                                          int totalPages) {
        return PageResultVO.<EmojiPackItemVO>builder()
                .records(records)
                .page(page)
                .size(size)
                .total(total)
                .totalPages(totalPages)
                .hasMore((long) page * size < total)
                .build();
    }
}
