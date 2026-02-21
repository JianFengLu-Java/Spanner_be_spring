package com.lujianfeng.spanner.service;

import com.lujianfeng.spanner.dto.cloud.CloudDocSaveRequestDTO;
import com.lujianfeng.spanner.dto.cloud.CloudDocShareCreateRequestDTO;
import com.lujianfeng.spanner.entity.cloud.CloudDocEntity;
import com.lujianfeng.spanner.entity.cloud.CloudDocShareEntity;
import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.entity.user.UserRelation;
import com.lujianfeng.spanner.entity.user.UserRelationEnum;
import com.lujianfeng.spanner.repository.CloudDocRepository;
import com.lujianfeng.spanner.repository.CloudDocShareRepository;
import com.lujianfeng.spanner.repository.UserRelationRepository;
import com.lujianfeng.spanner.repository.UserRepository;
import com.lujianfeng.spanner.vo.cloud.CloudDocDetailVO;
import com.lujianfeng.spanner.vo.cloud.CloudDocSaveResponseVO;
import com.lujianfeng.spanner.vo.cloud.CloudDocShareCreateResultVO;
import com.lujianfeng.spanner.vo.cloud.CloudDocShareRevokeResultVO;
import com.lujianfeng.spanner.vo.cloud.CloudDocShareViewVO;
import com.lujianfeng.spanner.vo.cloud.CloudDocReceivedShareItemVO;
import com.lujianfeng.spanner.vo.cloud.CloudDocSummaryVO;
import com.lujianfeng.spanner.vo.user.PageResultVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class CloudDocService {

    private static final String DEFAULT_TITLE = "未标题云文档";
    private static final String DEFAULT_CONTENT_JSON = "{\"type\":\"doc\",\"content\":[]}";
    private static final DateTimeFormatter ID_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String SHARE_MODE_READONLY = "READONLY";
    private static final String SHARE_MODE_COLLAB = "COLLAB";
    private static final int DEFAULT_SHARE_EXPIRE_HOURS = 24 * 7;
    private static final int MAX_SHARE_EXPIRE_HOURS = 24 * 30;

    private final CloudDocRepository cloudDocRepository;
    private final CloudDocShareRepository cloudDocShareRepository;
    private final UserRepository userRepository;
    private final UserRelationRepository userRelationRepository;
    private final CloudDocCollabWsService cloudDocCollabWsService;

    public CloudDocService(CloudDocRepository cloudDocRepository,
                           CloudDocShareRepository cloudDocShareRepository,
                           UserRepository userRepository,
                           UserRelationRepository userRelationRepository,
                           CloudDocCollabWsService cloudDocCollabWsService) {
        this.cloudDocRepository = cloudDocRepository;
        this.cloudDocShareRepository = cloudDocShareRepository;
        this.userRepository = userRepository;
        this.userRelationRepository = userRelationRepository;
        this.cloudDocCollabWsService = cloudDocCollabWsService;
    }

    public PageResultVO<CloudDocSummaryVO> listMyDocs(UserEntity currentUser,
                                                      Integer page,
                                                      Integer size,
                                                      String keywordRaw,
                                                      String sortRaw) {
        String account = requireAccount(currentUser);
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        String keyword = trim(keywordRaw);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, parseSort(sortRaw));

        Page<CloudDocEntity> pageResult;
        if (keyword == null) {
            pageResult = cloudDocRepository.findByOwnerAccountAndDeletedFalse(account, pageable);
        } else {
            pageResult = cloudDocRepository.findByOwnerAccountAndDeletedFalseAndTitleContainingIgnoreCase(account, keyword, pageable);
        }

        return PageResultVO.<CloudDocSummaryVO>builder()
                .records(pageResult.getContent().stream().map(this::toSummaryVO).toList())
                .page(safePage)
                .size(safeSize)
                .total(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .hasMore(pageResult.hasNext())
                .build();
    }

    @Transactional
    public CloudDocDetailVO createDoc(UserEntity currentUser, String titleRaw) {
        String account = requireAccount(currentUser);
        String title = normalizeTitle(titleRaw);
        LocalDateTime now = LocalDateTime.now();

        CloudDocEntity doc = new CloudDocEntity();
        doc.setDocId(generateDocId());
        doc.setOwnerAccount(account);
        doc.setTitle(title);
        doc.setContentHtml("<p></p>");
        doc.setContentJson(DEFAULT_CONTENT_JSON);
        doc.setSnippet("");
        doc.setVersion(1L);
        doc.setDeleted(false);
        doc.setCreatedAt(now);
        doc.setUpdatedAt(now);
        doc.setLastSavedAt(now);

        return toDetailVO(cloudDocRepository.save(doc), account);
    }

    public CloudDocDetailVO getDocDetail(UserEntity currentUser, String docIdRaw) {
        String account = requireAccount(currentUser);
        String docId = normalizeDocId(docIdRaw);
        CloudDocEntity doc = cloudDocRepository.findByDocIdAndDeletedFalse(docId)
                .orElseThrow(() -> new IllegalStateException("文档不存在"));
        boolean editable = canEditDoc(account, doc);
        boolean readable = editable || hasReadableShare(account, docId);
        if (!readable) {
            throw new IllegalStateException("文档不存在");
        }
        return toDetailVO(doc, account, editable);
    }

    @Transactional
    public CloudDocSaveResponseVO saveDoc(UserEntity currentUser, String docIdRaw, CloudDocSaveRequestDTO requestDTO) {
        String account = requireAccount(currentUser);
        String docId = normalizeDocId(docIdRaw);
        if (requestDTO == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (requestDTO.getBaseVersion() == null || requestDTO.getBaseVersion() < 1) {
            throw new IllegalArgumentException("baseVersion 非法");
        }

        CloudDocEntity doc = cloudDocRepository.findByDocIdAndDeletedFalse(docId)
                .orElseThrow(() -> new IllegalStateException("文档不存在"));
        if (!canEditDoc(account, doc)) {
            throw new IllegalStateException("无权限编辑该文档");
        }

        long dbVersion = doc.getVersion();
        long baseVersion = requestDTO.getBaseVersion();
        if (baseVersion < dbVersion) {
            throw new CloudDocVersionConflictException(dbVersion, doc.getUpdatedAt());
        }

        String nextTitle = normalizeTitle(requestDTO.getTitle());
        String contentHtml = sanitizeHtml(requestDTO.getContentHtml());
        String contentJson = normalizeContentJson(requestDTO.getContentJson());
        LocalDateTime now = LocalDateTime.now();

        doc.setTitle(nextTitle);
        doc.setContentHtml(contentHtml);
        doc.setContentJson(contentJson);
        doc.setSnippet(toSnippet(contentHtml));
        // 允许 baseVersion 前移到 DB 当前版本之上，减少协同内存线与 DB 暂时不一致导致的误冲突
        if (doc.getVersion() < baseVersion) {
            doc.setVersion(baseVersion);
        }
        doc.setVersion(doc.getVersion() + 1);
        doc.setUpdatedAt(now);
        doc.setLastSavedAt(now);

        CloudDocEntity saved = cloudDocRepository.save(doc);
        cloudDocCollabWsService.onPersisted(docId, saved.getVersion());

        return CloudDocSaveResponseVO.builder()
                .id(saved.getDocId())
                .updatedAt(formatUtc(saved.getUpdatedAt()))
                .lastSavedAt(formatUtc(saved.getLastSavedAt()))
                .version(saved.getVersion())
                .build();
    }

    @Transactional
    public void deleteDoc(UserEntity currentUser, String docIdRaw) {
        String account = requireAccount(currentUser);
        String docId = normalizeDocId(docIdRaw);

        CloudDocEntity doc = cloudDocRepository.findByDocIdAndOwnerAccountAndDeletedFalse(docId, account).orElse(null);
        if (doc == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        doc.setDeleted(true);
        doc.setUpdatedAt(now);
        doc.setLastSavedAt(now);
        cloudDocRepository.save(doc);
    }

    @Transactional
    public CloudDocShareCreateResultVO shareDocToFriend(UserEntity currentUser,
                                                        String docIdRaw,
                                                        CloudDocShareCreateRequestDTO requestDTO) {
        String account = requireAccount(currentUser);
        String docId = normalizeDocId(docIdRaw);
        if (requestDTO == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        String friendAccount = trim(requestDTO.getFriendAccount());
        if (friendAccount == null) {
            throw new IllegalArgumentException("friendAccount 不能为空");
        }
        if (account.equals(friendAccount)) {
            throw new IllegalArgumentException("不能分享给自己");
        }

        CloudDocEntity doc = cloudDocRepository.findByDocIdAndOwnerAccountAndDeletedFalse(docId, account)
                .orElseThrow(() -> new IllegalStateException("文档不存在"));

        UserEntity ownerUser = userRepository.findByAccount(account);
        UserEntity friendUser = userRepository.findByAccount(friendAccount);
        if (ownerUser == null || friendUser == null) {
            throw new IllegalArgumentException("好友账号不存在");
        }
        if (!isFriendAccepted(ownerUser, friendUser)) {
            throw new IllegalStateException("仅支持分享给好友");
        }

        int expireHours = normalizeExpireHours(requestDTO.getExpireHours());
        String shareMode = normalizeShareMode(requestDTO.getShareMode());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = now.plusHours(expireHours);

        CloudDocShareEntity share = new CloudDocShareEntity();
        share.setShareNo(generateShareNo());
        share.setDocId(doc.getDocId());
        share.setOwnerAccount(account);
        share.setFriendAccount(friendAccount);
        share.setStatus("ACTIVE");
        share.setShareMode(shareMode);
        share.setExpireAt(expireAt);
        share.setCreatedAt(now);
        share.setUpdatedAt(now);

        CloudDocShareEntity saved = cloudDocShareRepository.save(share);

        return CloudDocShareCreateResultVO.builder()
                .shareNo(saved.getShareNo())
                .docId(saved.getDocId())
                .friendAccount(saved.getFriendAccount())
                .shareMode(normalizeShareModeForOutput(saved.getShareMode()))
                .createdAt(formatUtc(saved.getCreatedAt()))
                .expireAt(formatUtc(saved.getExpireAt()))
                .sharePath("/cloud-docs/shares/" + saved.getShareNo())
                .build();
    }

    @Transactional
    public CloudDocShareViewVO getSharedDoc(UserEntity currentUser, String shareNoRaw) {
        String account = requireAccount(currentUser);
        String shareNo = normalizeShareNo(shareNoRaw);
        CloudDocShareEntity share = cloudDocShareRepository.findByShareNo(shareNo)
                .orElseThrow(() -> new IllegalStateException("分享不存在"));

        if (!"ACTIVE".equalsIgnoreCase(share.getStatus())) {
            throw new IllegalStateException("分享已失效");
        }
        if (!(account.equals(share.getFriendAccount()) || account.equals(share.getOwnerAccount()))) {
            throw new IllegalStateException("无权限查看该分享");
        }

        LocalDateTime now = LocalDateTime.now();
        if (share.getExpireAt() != null && now.isAfter(share.getExpireAt())) {
            throw new IllegalStateException("分享已过期");
        }

        CloudDocEntity doc = cloudDocRepository.findByDocIdAndDeletedFalse(share.getDocId())
                .orElseThrow(() -> new IllegalStateException("文档不存在"));

        if (account.equals(share.getFriendAccount())) {
            share.setLastViewedAt(now);
            cloudDocShareRepository.save(share);
        }

        String shareMode = normalizeShareModeForOutput(share.getShareMode());
        boolean collab = SHARE_MODE_COLLAB.equalsIgnoreCase(shareMode);
        CloudDocDetailVO docDetail = CloudDocDetailVO.builder()
                .id(doc.getDocId())
                .title(doc.getTitle())
                .contentHtml(doc.getContentHtml())
                .contentJson(doc.getContentJson())
                .createdAt(formatUtc(doc.getCreatedAt()))
                .updatedAt(formatUtc(doc.getUpdatedAt()))
                .lastSavedAt(formatUtc(doc.getLastSavedAt()))
                .version(doc.getVersion())
                .ownerAccount(doc.getOwnerAccount())
                .editable(account.equals(doc.getOwnerAccount()) || (account.equals(share.getFriendAccount()) && collab))
                .build();

        return CloudDocShareViewVO.builder()
                .shareNo(share.getShareNo())
                .shareMode(shareMode)
                .collaborative(collab)
                .doc(docDetail)
                .build();
    }

    @Transactional
    public CloudDocShareRevokeResultVO revokeShare(UserEntity currentUser, String shareNoRaw) {
        String account = requireAccount(currentUser);
        String shareNo = normalizeShareNo(shareNoRaw);
        CloudDocShareEntity share = cloudDocShareRepository.findByShareNoAndOwnerAccount(shareNo, account)
                .orElseThrow(() -> new IllegalStateException("分享不存在"));

        LocalDateTime now = LocalDateTime.now();
        if (!"REVOKED".equalsIgnoreCase(share.getStatus())) {
            share.setStatus("REVOKED");
            share.setUpdatedAt(now);
            cloudDocShareRepository.save(share);
        }

        return CloudDocShareRevokeResultVO.builder()
                .shareNo(share.getShareNo())
                .revoked(true)
                .revokedAt(formatUtc(now))
                .build();
    }

    @Transactional(readOnly = true)
    public PageResultVO<CloudDocReceivedShareItemVO> listReceivedShares(UserEntity currentUser,
                                                                        Integer page,
                                                                        Integer size,
                                                                        String statusRaw) {
        String account = requireAccount(currentUser);
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        String statusFilter = normalizeShareStatus(statusRaw);

        List<CloudDocShareEntity> shares = cloudDocShareRepository.findByFriendAccountOrderByCreatedAtDesc(account);
        List<String> docIds = shares.stream().map(CloudDocShareEntity::getDocId).distinct().toList();
        Map<String, CloudDocEntity> docById = new HashMap<>();
        if (!docIds.isEmpty()) {
            cloudDocRepository.findByDocIdInAndDeletedFalse(docIds).forEach(doc -> docById.put(doc.getDocId(), doc));
        }

        LocalDateTime now = LocalDateTime.now();
        List<CloudDocReceivedShareItemVO> records = shares.stream()
                .map(share -> toReceivedShareItem(share, docById.get(share.getDocId()), now))
                .filter(item -> statusFilter == null || statusFilter.equalsIgnoreCase(item.getStatus()))
                .toList();

        int total = records.size();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        int fromIndex = Math.min((safePage - 1) * safeSize, total);
        int toIndex = Math.min(fromIndex + safeSize, total);
        List<CloudDocReceivedShareItemVO> pageRecords = records.subList(fromIndex, toIndex);

        return PageResultVO.<CloudDocReceivedShareItemVO>builder()
                .records(pageRecords)
                .page(safePage)
                .size(safeSize)
                .total(total)
                .totalPages(totalPages)
                .hasMore(safePage < totalPages)
                .build();
    }

    private CloudDocSummaryVO toSummaryVO(CloudDocEntity doc) {
        return CloudDocSummaryVO.builder()
                .id(doc.getDocId())
                .title(doc.getTitle())
                .snippet(doc.getSnippet())
                .createdAt(formatUtc(doc.getCreatedAt()))
                .updatedAt(formatUtc(doc.getUpdatedAt()))
                .lastSavedAt(formatUtc(doc.getLastSavedAt()))
                .version(doc.getVersion())
                .deleted(Boolean.TRUE.equals(doc.getDeleted()))
                .build();
    }

    private CloudDocReceivedShareItemVO toReceivedShareItem(CloudDocShareEntity share,
                                                            CloudDocEntity doc,
                                                            LocalDateTime now) {
        boolean expired = isShareExpired(share, now);
        String status = resolveShareStatus(share, expired);
        return CloudDocReceivedShareItemVO.builder()
                .shareNo(share.getShareNo())
                .docId(share.getDocId())
                .title(doc == null ? "文档不存在或已删除" : doc.getTitle())
                .snippet(doc == null ? "" : doc.getSnippet())
                .ownerAccount(share.getOwnerAccount())
                .shareMode(normalizeShareModeForOutput(share.getShareMode()))
                .status(status)
                .expired(expired)
                .createdAt(formatUtc(share.getCreatedAt()))
                .expireAt(formatUtc(share.getExpireAt()))
                .lastViewedAt(formatUtc(share.getLastViewedAt()))
                .build();
    }

    private CloudDocDetailVO toDetailVO(CloudDocEntity doc, String account) {
        return toDetailVO(doc, account, account.equals(doc.getOwnerAccount()) && !Boolean.TRUE.equals(doc.getDeleted()));
    }

    private CloudDocDetailVO toDetailVO(CloudDocEntity doc, String account, boolean editable) {
        return CloudDocDetailVO.builder()
                .id(doc.getDocId())
                .title(doc.getTitle())
                .contentHtml(doc.getContentHtml())
                .contentJson(doc.getContentJson())
                .createdAt(formatUtc(doc.getCreatedAt()))
                .updatedAt(formatUtc(doc.getUpdatedAt()))
                .lastSavedAt(formatUtc(doc.getLastSavedAt()))
                .version(doc.getVersion())
                .ownerAccount(doc.getOwnerAccount())
                .editable(editable)
                .build();
    }

    private String requireAccount(UserEntity currentUser) {
        if (currentUser == null || currentUser.getAccount() == null || currentUser.getAccount().isBlank()) {
            throw new SecurityException("未登录");
        }
        return currentUser.getAccount();
    }

    private String normalizeDocId(String docIdRaw) {
        String docId = trim(docIdRaw);
        if (docId == null) {
            throw new IllegalArgumentException("docId 不能为空");
        }
        return docId;
    }

    private String normalizeTitle(String titleRaw) {
        String title = trim(titleRaw);
        if (title == null) {
            return DEFAULT_TITLE;
        }
        if (title.length() > 128) {
            throw new IllegalArgumentException("title 长度不能超过 128");
        }
        return title;
    }

    private String normalizeContentJson(String contentJsonRaw) {
        String contentJson = trim(contentJsonRaw);
        if (contentJson == null) {
            return DEFAULT_CONTENT_JSON;
        }
        if (contentJson.length() > 2_000_000) {
            throw new IllegalArgumentException("contentJson 长度不能超过 2000000");
        }
        return contentJson;
    }

    private String sanitizeHtml(String rawHtml) {
        if (rawHtml == null || rawHtml.isBlank()) {
            return "<p></p>";
        }
        String sanitized = rawHtml
                .replaceAll("(?is)<script[^>]*>.*?</script>", "")
                .replaceAll("(?is)<style[^>]*>.*?</style>", "")
                .replaceAll("(?i)on\\w+\\s*=\\s*\"[^\"]*\"", "")
                .replaceAll("(?i)on\\w+\\s*=\\s*'[^']*'", "")
                .replaceAll("(?i)javascript:", "");
        if (sanitized.length() > 2_000_000) {
            throw new IllegalArgumentException("contentHtml 长度不能超过 2000000");
        }
        return sanitized;
    }

    private String toSnippet(String contentHtml) {
        if (contentHtml == null || contentHtml.isBlank()) {
            return "";
        }
        String plain = contentHtml.replaceAll("(?is)<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (plain.length() <= 120) {
            return plain;
        }
        return plain.substring(0, 120);
    }

    private Sort parseSort(String sortRaw) {
        String sort = trim(sortRaw);
        if (sort == null || "updatedAt_desc".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "updatedAt");
        }
        if ("updatedAt_asc".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "updatedAt");
        }
        if ("createdAt_desc".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        if ("createdAt_asc".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "createdAt");
        }
        throw new IllegalArgumentException("sort 参数非法");
    }

    private boolean isFriendAccepted(UserEntity ownerUser, UserEntity friendUser) {
        return userRelationRepository.findPairRelations(ownerUser, friendUser).stream()
                .map(UserRelation::getRelationType)
                .anyMatch(type -> type == UserRelationEnum.ACCEPTED);
    }

    private int normalizeExpireHours(Integer expireHours) {
        if (expireHours == null) {
            return DEFAULT_SHARE_EXPIRE_HOURS;
        }
        if (expireHours < 1 || expireHours > MAX_SHARE_EXPIRE_HOURS) {
            throw new IllegalArgumentException("expireHours 范围必须在 1-720 之间");
        }
        return expireHours;
    }

    private String normalizeShareMode(String shareModeRaw) {
        String shareMode = trim(shareModeRaw);
        if (shareMode == null) {
            return SHARE_MODE_READONLY;
        }
        String normalized = shareMode.toUpperCase(Locale.ROOT);
        if (!SHARE_MODE_READONLY.equals(normalized) && !SHARE_MODE_COLLAB.equals(normalized)) {
            throw new IllegalArgumentException("shareMode 仅支持 READONLY/COLLAB");
        }
        return normalized;
    }

    private String normalizeShareModeForOutput(String shareModeRaw) {
        if (shareModeRaw == null || shareModeRaw.isBlank()) {
            return SHARE_MODE_READONLY;
        }
        String normalized = shareModeRaw.toUpperCase(Locale.ROOT);
        if (SHARE_MODE_COLLAB.equals(normalized)) {
            return SHARE_MODE_COLLAB;
        }
        return SHARE_MODE_READONLY;
    }

    private boolean canEditDoc(String account, CloudDocEntity doc) {
        if (account.equals(doc.getOwnerAccount())) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now();
        return cloudDocShareRepository
                .findByDocIdAndFriendAccountAndStatusAndShareMode(doc.getDocId(), account, "ACTIVE", SHARE_MODE_COLLAB)
                .stream()
                .anyMatch(share -> share.getExpireAt() == null || now.isBefore(share.getExpireAt()));
    }

    private boolean hasReadableShare(String account, String docId) {
        LocalDateTime now = LocalDateTime.now();
        return cloudDocShareRepository.findByDocIdAndFriendAccountAndStatus(docId, account, "ACTIVE")
                .stream()
                .anyMatch(share -> share.getExpireAt() == null || now.isBefore(share.getExpireAt()));
    }

    private String normalizeShareStatus(String statusRaw) {
        String status = trim(statusRaw);
        if (status == null) {
            return null;
        }
        String normalized = status.toUpperCase(Locale.ROOT);
        if (!"ACTIVE".equals(normalized) && !"EXPIRED".equals(normalized) && !"REVOKED".equals(normalized)) {
            throw new IllegalArgumentException("status 仅支持 ACTIVE/EXPIRED/REVOKED");
        }
        return normalized;
    }

    private boolean isShareExpired(CloudDocShareEntity share, LocalDateTime now) {
        return share.getExpireAt() != null && now.isAfter(share.getExpireAt());
    }

    private String resolveShareStatus(CloudDocShareEntity share, boolean expired) {
        if ("REVOKED".equalsIgnoreCase(share.getStatus())) {
            return "REVOKED";
        }
        if (expired) {
            return "EXPIRED";
        }
        return "ACTIVE";
    }

    private String generateDocId() {
        String day = LocalDateTime.now().format(ID_DATE_FORMAT);
        for (int i = 0; i < 5; i++) {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toLowerCase(Locale.ROOT);
            String docId = "doc_" + day + "_" + suffix;
            if (!cloudDocRepository.existsByDocId(docId)) {
                return docId;
            }
        }
        return "doc_" + day + "_" + UUID.randomUUID().toString().replace("-", "").toLowerCase(Locale.ROOT);
    }

    private String generateShareNo() {
        String day = LocalDateTime.now().format(ID_DATE_FORMAT);
        for (int i = 0; i < 5; i++) {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toLowerCase(Locale.ROOT);
            String shareNo = "share_" + day + "_" + suffix;
            if (!cloudDocShareRepository.existsByShareNo(shareNo)) {
                return shareNo;
            }
        }
        return "share_" + day + "_" + UUID.randomUUID().toString().replace("-", "").toLowerCase(Locale.ROOT);
    }

    private String formatUtc(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC)
                .toInstant()
                .toString();
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeShareNo(String shareNoRaw) {
        String shareNo = trim(shareNoRaw);
        if (shareNo == null) {
            throw new IllegalArgumentException("shareNo 不能为空");
        }
        return shareNo;
    }
}
