package com.lujianfeng.spanner.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lujianfeng.spanner.dto.moment.MomentCommentCreateRequestDTO;
import com.lujianfeng.spanner.dto.moment.MomentCreateRequestDTO;
import com.lujianfeng.spanner.entity.moment.MomentCommentEntity;
import com.lujianfeng.spanner.entity.moment.MomentEntity;
import com.lujianfeng.spanner.entity.moment.MomentLikeEntity;
import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.entity.user.UserRelation;
import com.lujianfeng.spanner.entity.user.UserRelationEnum;
import com.lujianfeng.spanner.repository.MomentCommentRepository;
import com.lujianfeng.spanner.repository.MomentLikeRepository;
import com.lujianfeng.spanner.repository.MomentRepository;
import com.lujianfeng.spanner.repository.UserRelationRepository;
import com.lujianfeng.spanner.service.task.TaskRewardService;
import com.lujianfeng.spanner.service.service.MomentService;
import com.lujianfeng.spanner.service.service.UserService;
import com.lujianfeng.spanner.vo.moment.CursorPageVO;
import com.lujianfeng.spanner.vo.moment.MomentCommentItemVO;
import com.lujianfeng.spanner.vo.moment.MomentItemVO;
import com.lujianfeng.spanner.vo.moment.MomentLikeUserVO;
import com.lujianfeng.spanner.vo.moment.MomentUserVO;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class MomentServiceImpl implements MomentService {

    private final MomentRepository momentRepository;
    private final MomentLikeRepository momentLikeRepository;
    private final MomentCommentRepository momentCommentRepository;
    private final UserRelationRepository userRelationRepository;
    private final UserService userService;
    private final TaskRewardService taskRewardService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MomentServiceImpl(MomentRepository momentRepository,
                             MomentLikeRepository momentLikeRepository,
                             MomentCommentRepository momentCommentRepository,
                             UserRelationRepository userRelationRepository,
                             UserService userService,
                             TaskRewardService taskRewardService) {
        this.momentRepository = momentRepository;
        this.momentLikeRepository = momentLikeRepository;
        this.momentCommentRepository = momentCommentRepository;
        this.userRelationRepository = userRelationRepository;
        this.userService = userService;
        this.taskRewardService = taskRewardService;
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPageVO<MomentItemVO> listMoments(String tab, String keyword, String cursor, Integer size, Double lat, Double lng) {
        UserEntity currentUser = requireCurrentUser();
        int pageSize = normalizePageSize(size);
        CursorToken cursorToken = parseCursorToken(cursor, false);
        validateTab(tab);

        Pageable pageable = PageRequest.of(0, pageSize + 1, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        List<MomentEntity> entities = momentRepository.findAll(buildMomentListSpec(currentUser, tab, keyword, cursorToken), pageable).getContent();

        boolean hasMore = entities.size() > pageSize;
        if (hasMore) {
            entities = entities.subList(0, pageSize);
        }

        Set<String> likedMomentIds = loadLikedMomentIds(currentUser, entities);
        List<MomentItemVO> records = entities.stream()
                .map(entity -> toMomentItem(entity, currentUser, likedMomentIds.contains(entity.getId())))
                .toList();

        String nextCursor = null;
        if (hasMore && !entities.isEmpty()) {
            MomentEntity tail = entities.get(entities.size() - 1);
            nextCursor = buildCursor(tail.getCreatedAt(), tail.getId());
        }

        return CursorPageVO.<MomentItemVO>builder()
                .records(records)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MomentItemVO getMomentDetail(String momentId) {
        UserEntity currentUser = requireCurrentUser();
        MomentEntity moment = getMomentOrThrow(momentId);
        boolean isLiked = momentLikeRepository.existsByMomentAndUser(moment, currentUser);
        return toMomentItem(moment, currentUser, isLiked);
    }

    @Override
    @Transactional
    public MomentItemVO createMoment(MomentCreateRequestDTO request) {
        UserEntity currentUser = requireCurrentUser();
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }

        String title = safeTrim(request.getTitle());
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("title 不能为空");
        }
        if (title.length() > 80) {
            throw new IllegalArgumentException("title 长度不能超过 80");
        }

        String contentText = safeTrim(request.getContentText());
        String contentHtml = safeTrim(request.getContentHtml());
        if ((contentText == null || contentText.isEmpty()) && (contentHtml == null || contentHtml.isEmpty())) {
            throw new IllegalArgumentException("contentText 与 contentHtml 至少有一个非空");
        }

        List<String> images = normalizeImages(request.getImages());

        MomentEntity entity = new MomentEntity();
        entity.setId(generateMomentId());
        entity.setTitle(title);
        entity.setAuthor(currentUser);
        entity.setContentText(contentText);
        entity.setContentHtml(contentHtml);
        entity.setImagesJson(toImagesJson(images));
        entity.setCover(images.isEmpty() ? null : images.get(0));
        entity.setLikesCount(0L);
        entity.setCommentsCount(0L);

        MomentEntity saved = momentRepository.save(entity);
        try {
            taskRewardService.onMomentCreated(saved.getId(), currentUser.getId());
        } catch (Exception ignored) {
            // 奖励失败不阻塞发帖主流程
        }
        return toMomentItem(saved, currentUser, false);
    }

    @Override
    @Transactional
    public MomentItemVO updateMoment(String momentId, MomentCreateRequestDTO request) {
        UserEntity currentUser = requireCurrentUser();
        MomentEntity moment = getMomentOrThrow(momentId);
        ensureMomentOwner(moment, currentUser);

        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }

        String title = safeTrim(request.getTitle());
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("title 不能为空");
        }
        if (title.length() > 80) {
            throw new IllegalArgumentException("title 长度不能超过 80");
        }

        String contentText = safeTrim(request.getContentText());
        String contentHtml = safeTrim(request.getContentHtml());
        if ((contentText == null || contentText.isEmpty()) && (contentHtml == null || contentHtml.isEmpty())) {
            throw new IllegalArgumentException("contentText 与 contentHtml 至少有一个非空");
        }

        List<String> images = normalizeImages(request.getImages());

        moment.setTitle(title);
        moment.setContentText(contentText);
        moment.setContentHtml(contentHtml);
        moment.setImagesJson(toImagesJson(images));
        moment.setCover(images.isEmpty() ? null : images.get(0));

        MomentEntity saved = momentRepository.save(moment);
        boolean isLiked = momentLikeRepository.existsByMomentAndUser(saved, currentUser);
        return toMomentItem(saved, currentUser, isLiked);
    }

    @Override
    @Transactional
    public void deleteMoment(String momentId) {
        UserEntity currentUser = requireCurrentUser();
        MomentEntity moment = getMomentOrThrow(momentId);
        ensureMomentOwner(moment, currentUser);
        momentLikeRepository.deleteByMoment(moment);
        momentCommentRepository.deleteByMoment(moment);
        momentRepository.delete(moment);
    }

    @Override
    @Transactional
    public Map<String, Object> likeMoment(String momentId) {
        UserEntity currentUser = requireCurrentUser();
        MomentEntity moment = getMomentOrThrow(momentId);

        if (!momentLikeRepository.existsByMomentAndUser(moment, currentUser)) {
            try {
                MomentLikeEntity like = new MomentLikeEntity();
                like.setMoment(moment);
                like.setUser(currentUser);
                momentLikeRepository.save(like);
                moment.setLikesCount(moment.getLikesCount() + 1);
                momentRepository.save(moment);
            } catch (Exception ignored) {
                // 并发下唯一键冲突时按已点赞处理，保持幂等
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("liked", true);
        data.put("likes", momentRepository.findById(moment.getId()).map(MomentEntity::getLikesCount).orElse(moment.getLikesCount()));
        return data;
    }

    @Override
    @Transactional
    public Map<String, Object> unlikeMoment(String momentId) {
        UserEntity currentUser = requireCurrentUser();
        MomentEntity moment = getMomentOrThrow(momentId);

        momentLikeRepository.findByMomentAndUser(moment, currentUser).ifPresent(like -> {
            momentLikeRepository.delete(like);
            Long newCount = Math.max(0L, moment.getLikesCount() - 1);
            moment.setLikesCount(newCount);
            momentRepository.save(moment);
        });

        Map<String, Object> data = new HashMap<>();
        data.put("liked", false);
        data.put("likes", momentRepository.findById(moment.getId()).map(MomentEntity::getLikesCount).orElse(moment.getLikesCount()));
        return data;
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPageVO<MomentCommentItemVO> listComments(String momentId, String cursor, Integer size, String parentCommentId, String sort) {
        requireCurrentUser();
        MomentEntity moment = getMomentOrThrow(momentId);
        int pageSize = normalizePageSize(size);
        CursorToken cursorToken = parseCursorToken(cursor, false);
        String normalizedSort = safeTrim(sort);
        if (normalizedSort == null || normalizedSort.isEmpty()) {
            normalizedSort = "latest";
        } else if (!"latest".equalsIgnoreCase(normalizedSort) && !"hot".equalsIgnoreCase(normalizedSort)) {
            throw new IllegalArgumentException("sort 仅支持 latest 或 hot");
        }

        Pageable pageable = PageRequest.of(0, pageSize + 1);
        String normalizedParentId = safeTrim(parentCommentId);
        boolean hasCursor = cursorToken.time() != null && cursorToken.id() != null;
        List<MomentCommentEntity> entities;
        if ("hot".equalsIgnoreCase(normalizedSort)) {
            if (hasCursor) {
                entities = momentCommentRepository.findHotPage(moment, normalizedParentId, cursorToken.time(), cursorToken.id(), pageable);
            } else {
                entities = momentCommentRepository.findHotFirstPage(moment, normalizedParentId, pageable);
            }
        } else {
            if (hasCursor) {
                entities = momentCommentRepository.findLatestPage(moment, normalizedParentId, cursorToken.time(), cursorToken.id(), pageable);
            } else {
                entities = momentCommentRepository.findLatestFirstPage(moment, normalizedParentId, pageable);
            }
        }

        boolean hasMore = entities.size() > pageSize;
        if (hasMore) {
            entities = entities.subList(0, pageSize);
        }

        List<MomentCommentItemVO> records = entities.stream()
                .map(entity -> toCommentItem(entity, moment.getId(), normalizedParentId == null ? countReplies(moment, entity.getId()) : 0L))
                .toList();

        String nextCursor = null;
        if (hasMore && !entities.isEmpty()) {
            MomentCommentEntity tail = entities.get(entities.size() - 1);
            nextCursor = buildCursor(tail.getCreatedAt(), tail.getId());
        }

        return CursorPageVO.<MomentCommentItemVO>builder()
                .records(records)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }

    @Override
    @Transactional
    public MomentCommentItemVO createComment(String momentId, MomentCommentCreateRequestDTO request) {
        UserEntity currentUser = requireCurrentUser();
        MomentEntity moment = getMomentOrThrow(momentId);

        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }

        String text = sanitizeCommentText(request.getText());
        if (text.isEmpty()) {
            throw new IllegalArgumentException("text 不能为空");
        }
        if (text.length() > 500) {
            throw new IllegalArgumentException("text 长度不能超过 500");
        }

        String parentCommentId = safeTrim(request.getParentCommentId());
        String replyToAccount = safeTrim(request.getReplyToAccount());
        if (parentCommentId != null) {
            MomentCommentEntity parent = momentCommentRepository.findByIdAndMoment(parentCommentId, moment)
                    .orElseThrow(() -> new IllegalStateException("评论不存在"));
            if (parent.getParentCommentId() != null) {
                throw new IllegalArgumentException("评论回复深度最多 2 层");
            }
            if (replyToAccount == null || replyToAccount.isEmpty()) {
                throw new IllegalArgumentException("replyToAccount 不能为空");
            }
        } else {
            replyToAccount = null;
        }

        MomentCommentEntity entity = new MomentCommentEntity();
        entity.setId(generateCommentId());
        entity.setMoment(moment);
        entity.setParentCommentId(parentCommentId);
        entity.setReplyToAccount(replyToAccount);
        entity.setAuthor(currentUser);
        entity.setText(text);
        entity.setLikesCount(0L);

        MomentCommentEntity saved = momentCommentRepository.save(entity);
        moment.setCommentsCount(moment.getCommentsCount() + 1);
        momentRepository.save(moment);
        try {
            taskRewardService.onCommentCreated(saved.getId(), moment.getId(), currentUser.getId(), text);
        } catch (Exception ignored) {
            // 奖励失败不阻塞回复主流程
        }

        return toCommentItem(saved, moment.getId(), 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPageVO<MomentLikeUserVO> listLikes(String momentId, String cursor, Integer size) {
        requireCurrentUser();
        MomentEntity moment = getMomentOrThrow(momentId);
        int pageSize = normalizePageSize(size);

        CursorToken cursorToken = parseCursorToken(cursor, true);
        Long cursorLikeId = null;
        if (cursorToken.id() != null && !cursorToken.id().isBlank()) {
            try {
                cursorLikeId = Long.parseLong(cursorToken.id());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("cursor 格式非法");
            }
        }

        Pageable pageable = PageRequest.of(0, pageSize + 1);
        List<MomentLikeEntity> likes = momentLikeRepository.findLikePage(moment, cursorToken.time(), cursorLikeId, pageable);

        boolean hasMore = likes.size() > pageSize;
        if (hasMore) {
            likes = likes.subList(0, pageSize);
        }

        List<MomentLikeUserVO> records = likes.stream()
                .map(this::toLikeUser)
                .toList();

        String nextCursor = null;
        if (hasMore && !likes.isEmpty()) {
            MomentLikeEntity tail = likes.get(likes.size() - 1);
            nextCursor = buildCursor(tail.getLikedAt(), String.valueOf(tail.getId()));
        }

        return CursorPageVO.<MomentLikeUserVO>builder()
                .records(records)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }

    private Specification<MomentEntity> buildMomentListSpec(UserEntity currentUser, String tab, String keyword, CursorToken cursorToken) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            String normalizedTab = safeTrim(tab);
            if (normalizedTab == null || normalizedTab.isEmpty()) {
                normalizedTab = "recommend";
            }

            if ("friends".equalsIgnoreCase(normalizedTab)) {
                Set<Long> visibleAuthorIds = loadFriendAndSelfIds(currentUser);
                predicates.add(root.get("author").get("id").in(visibleAuthorIds));
            }

            String kw = safeTrim(keyword);
            if (kw != null && !kw.isEmpty()) {
                String pattern = "%" + kw.toLowerCase(Locale.ROOT) + "%";
                Join<Object, Object> authorJoin = root.join("author");
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("contentText")), pattern),
                        cb.like(cb.lower(root.get("contentHtml")), pattern),
                        cb.like(cb.lower(authorJoin.get("realName")), pattern)
                ));
            }

            if (cursorToken.time() != null && cursorToken.id() != null) {
                predicates.add(cb.or(
                        cb.lessThan(root.get("createdAt"), cursorToken.time()),
                        cb.and(
                                cb.equal(root.get("createdAt"), cursorToken.time()),
                                cb.lessThan(root.get("id"), cursorToken.id())
                        )
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Set<Long> loadFriendAndSelfIds(UserEntity currentUser) {
        Set<Long> ids = new HashSet<>();
        ids.add(currentUser.getId());
        List<UserRelation> relations = userRelationRepository.findAllByOwnerAndRelationType(currentUser, UserRelationEnum.ACCEPTED);
        for (UserRelation relation : relations) {
            if (relation.getUser().getId().equals(currentUser.getId())) {
                ids.add(relation.getFriend().getId());
            } else if (relation.getFriend().getId().equals(currentUser.getId())) {
                ids.add(relation.getUser().getId());
            }
        }
        return ids;
    }

    private Set<String> loadLikedMomentIds(UserEntity currentUser, List<MomentEntity> moments) {
        if (moments.isEmpty()) {
            return Set.of();
        }
        List<String> ids = moments.stream().map(MomentEntity::getId).toList();
        return new HashSet<>(momentLikeRepository.findLikedMomentIdsByUserAndMomentIds(currentUser, ids));
    }

    private MomentItemVO toMomentItem(MomentEntity entity, UserEntity currentUser, boolean isLiked) {
        List<String> images = parseImagesJson(entity.getImagesJson());
        String cover = safeTrim(entity.getCover());
        if ((cover == null || cover.isEmpty()) && !images.isEmpty()) {
            cover = images.get(0);
        }

        List<MomentUserVO> likePreviewUsers = momentLikeRepository.findTop3ByMomentOrderByLikedAtDescIdDesc(entity)
                .stream()
                .map(like -> toUserVO(like.getUser()))
                .toList();

        return MomentItemVO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .cover(cover)
                .author(toUserVO(entity.getAuthor()))
                .content(entity.getContentText())
                .contentHtml(entity.getContentHtml())
                .images(images)
                .likes(entity.getLikesCount())
                .isLiked(isLiked)
                .likePreviewUsers(likePreviewUsers)
                .commentsCount(entity.getCommentsCount())
                .isFavorited(false)
                .friendStatusWithAuthor(resolveFriendStatus(currentUser, entity.getAuthor()))
                .timestamp(entity.getCreatedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private MomentCommentItemVO toCommentItem(MomentCommentEntity entity, String momentId, Long replyCount) {
        return MomentCommentItemVO.builder()
                .id(entity.getId())
                .momentId(momentId)
                .parentCommentId(entity.getParentCommentId())
                .replyToAccount(entity.getReplyToAccount())
                .author(toUserVO(entity.getAuthor()))
                .text(entity.getText())
                .likes(entity.getLikesCount())
                .isLiked(false)
                .replyCount(replyCount)
                .timestamp(entity.getCreatedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private MomentLikeUserVO toLikeUser(MomentLikeEntity likeEntity) {
        UserEntity user = likeEntity.getUser();
        return MomentLikeUserVO.builder()
                .account(user.getAccount())
                .name(user.getRealName())
                .avatar(user.getAvatarUrl())
                .likedAt(likeEntity.getLikedAt())
                .build();
    }

    private MomentUserVO toUserVO(UserEntity user) {
        return MomentUserVO.builder()
                .account(user.getAccount())
                .name(user.getRealName())
                .avatar(user.getAvatarUrl())
                .build();
    }

    private Long countReplies(MomentEntity moment, String parentCommentId) {
        return momentCommentRepository.countByMomentAndParentCommentId(moment, parentCommentId);
    }

    private String resolveFriendStatus(UserEntity currentUser, UserEntity author) {
        if (currentUser.getId().equals(author.getId())) {
            return "FRIEND";
        }

        UserRelation outbound = userRelationRepository.findByUserAndFriend(currentUser, author).orElse(null);
        if (outbound != null) {
            if (outbound.getRelationType() == UserRelationEnum.ACCEPTED) {
                return "FRIEND";
            }
            if (outbound.getRelationType() == UserRelationEnum.PENDING) {
                return "PENDING_OUTBOUND";
            }
        }

        UserRelation inbound = userRelationRepository.findByUserAndFriend(author, currentUser).orElse(null);
        if (inbound != null) {
            if (inbound.getRelationType() == UserRelationEnum.ACCEPTED) {
                return "FRIEND";
            }
            if (inbound.getRelationType() == UserRelationEnum.PENDING) {
                return "PENDING_INBOUND";
            }
        }

        return "NONE";
    }

    private void ensureMomentOwner(MomentEntity moment, UserEntity currentUser) {
        if (moment == null || currentUser == null) {
            throw new AccessDeniedException("无权限管理该动态");
        }
        if (!moment.getAuthor().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("无权限管理该动态");
        }
    }

    private String sanitizeCommentText(String raw) {
        String text = raw == null ? "" : raw.trim();
        text = text.replaceAll("(?is)<script.*?>.*?</script>", "");
        text = text.replaceAll("<[^>]+>", "");
        return text.trim();
    }

    private List<String> normalizeImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return List.of();
        }
        return images.stream()
                .map(this::safeTrim)
                .filter(v -> v != null && !v.isEmpty())
                .toList();
    }

    private String toImagesJson(List<String> images) {
        try {
            return objectMapper.writeValueAsString(images == null ? List.of() : images);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> parseImagesJson(String imagesJson) {
        if (imagesJson == null || imagesJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(imagesJson, new TypeReference<>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private CursorToken parseCursorToken(String cursor, boolean allowNumericId) {
        String value = safeTrim(cursor);
        if (value == null || value.isEmpty()) {
            return new CursorToken(null, null);
        }
        int split = value.indexOf('_');
        if (split <= 0 || split >= value.length() - 1) {
            throw new IllegalArgumentException("cursor 格式非法");
        }
        String timePart = value.substring(0, split);
        String idPart = value.substring(split + 1);
        try {
            Instant time = Instant.parse(timePart);
            if (!allowNumericId && idPart.isBlank()) {
                throw new IllegalArgumentException("cursor 格式非法");
            }
            return new CursorToken(time, idPart);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("cursor 格式非法");
        }
    }

    private String buildCursor(Instant time, String id) {
        if (time == null || id == null) {
            return null;
        }
        return time + "_" + id;
    }

    private int normalizePageSize(Integer size) {
        if (size == null) {
            return 20;
        }
        if (size < 1 || size > 50) {
            throw new IllegalArgumentException("size 必须在 1 到 50 之间");
        }
        return size;
    }

    private void validateTab(String tab) {
        String normalizedTab = safeTrim(tab);
        if (normalizedTab == null) {
            return;
        }
        if (!"recommend".equalsIgnoreCase(normalizedTab)
                && !"friends".equalsIgnoreCase(normalizedTab)
                && !"nearby".equalsIgnoreCase(normalizedTab)
                && !"trending".equalsIgnoreCase(normalizedTab)) {
            throw new IllegalArgumentException("tab 参数非法");
        }
    }

    private MomentEntity getMomentOrThrow(String momentId) {
        String id = safeTrim(momentId);
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("momentId 不能为空");
        }
        return momentRepository.findById(id).orElseThrow(() -> new IllegalStateException("动态不存在"));
    }

    private UserEntity requireCurrentUser() {
        UserEntity user = userService.getCurrentUserEntity();
        if (user == null) {
            throw new SecurityException("未登录");
        }
        return user;
    }

    private String safeTrim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String generateMomentId() {
        return "m_" + Instant.now().toEpochMilli() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String generateCommentId() {
        return "mc_" + Instant.now().toEpochMilli() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private record CursorToken(Instant time, String id) {
    }
}
