package com.lujianfeng.spanner.service;

import com.lujianfeng.spanner.dto.cloud.CloudDocSaveRequestDTO;
import com.lujianfeng.spanner.entity.cloud.CloudDocEntity;
import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.repository.CloudDocRepository;
import com.lujianfeng.spanner.vo.cloud.CloudDocDetailVO;
import com.lujianfeng.spanner.vo.cloud.CloudDocSaveResponseVO;
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
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class CloudDocService {

    private static final String DEFAULT_TITLE = "未标题云文档";
    private static final String DEFAULT_CONTENT_JSON = "{\"type\":\"doc\",\"content\":[]}";
    private static final DateTimeFormatter ID_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CloudDocRepository cloudDocRepository;

    public CloudDocService(CloudDocRepository cloudDocRepository) {
        this.cloudDocRepository = cloudDocRepository;
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
        CloudDocEntity doc = cloudDocRepository.findByDocIdAndOwnerAccountAndDeletedFalse(docId, account)
                .orElseThrow(() -> new IllegalStateException("文档不存在"));
        return toDetailVO(doc, account);
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

        CloudDocEntity doc = cloudDocRepository.findByDocIdAndOwnerAccountAndDeletedFalse(docId, account)
                .orElseThrow(() -> new IllegalStateException("文档不存在"));

        if (!Objects.equals(requestDTO.getBaseVersion(), doc.getVersion())) {
            throw new CloudDocVersionConflictException(doc.getVersion(), doc.getUpdatedAt());
        }

        String nextTitle = normalizeTitle(requestDTO.getTitle());
        String contentHtml = sanitizeHtml(requestDTO.getContentHtml());
        String contentJson = normalizeContentJson(requestDTO.getContentJson());
        LocalDateTime now = LocalDateTime.now();

        doc.setTitle(nextTitle);
        doc.setContentHtml(contentHtml);
        doc.setContentJson(contentJson);
        doc.setSnippet(toSnippet(contentHtml));
        doc.setVersion(doc.getVersion() + 1);
        doc.setUpdatedAt(now);
        doc.setLastSavedAt(now);

        CloudDocEntity saved = cloudDocRepository.save(doc);

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

    private CloudDocDetailVO toDetailVO(CloudDocEntity doc, String account) {
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
                .editable(account.equals(doc.getOwnerAccount()) && !Boolean.TRUE.equals(doc.getDeleted()))
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
}
