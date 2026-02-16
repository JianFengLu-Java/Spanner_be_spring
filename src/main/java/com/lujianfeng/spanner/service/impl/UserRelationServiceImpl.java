package com.lujianfeng.spanner.service.impl;

import com.lujianfeng.spanner.entity.user.FriendRequestDirectionEnum;
import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.entity.user.UserRelation;
import com.lujianfeng.spanner.entity.user.UserRelationEnum;
import com.lujianfeng.spanner.event.friend.FriendRequestEventType;
import com.lujianfeng.spanner.event.friend.FriendRequestWsDomainEvent;
import com.lujianfeng.spanner.event.message.FriendAcceptedGreetingDomainEvent;
import com.lujianfeng.spanner.repository.UserRelationRepository;
import com.lujianfeng.spanner.service.service.UserRelationService;
import com.lujianfeng.spanner.vo.user.FriendRequestActionResultVO;
import com.lujianfeng.spanner.vo.user.FriendRequestHistoryItemVO;
import com.lujianfeng.spanner.vo.user.PageResultVO;
import com.lujianfeng.spanner.vo.ws.FriendRequestEventVO;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserRelationServiceImpl implements UserRelationService {

    private static final int VERIFY_MESSAGE_MAX_LEN = 200;
    private static final String SOURCE_ACCOUNT_SEARCH = "ACCOUNT_SEARCH";
    private static final String SOURCE_SYSTEM_MIRROR = "SYSTEM_MIRROR";
    private static final String OPERATOR_SYSTEM = "SYSTEM";
    private static final String FRIEND_ACCEPTED_GREETING = "哈喽我们已经是好友了，快来一起聊天吧！";
    private static final Duration APPLY_RATE_LIMIT_WINDOW = Duration.ofSeconds(3);
    private static final ConcurrentHashMap<String, Instant> APPLY_RATE_LIMIT_CACHE = new ConcurrentHashMap<>();

    private final UserRelationRepository userRelationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UserRelationServiceImpl(UserRelationRepository userRelationRepository,
                                   ApplicationEventPublisher eventPublisher) {
        this.userRelationRepository = userRelationRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public FriendRequestActionResultVO applyRelation(UserEntity userEntity, UserEntity friendEntity, String verificationMessage) {
        if (userEntity.getId().equals(friendEntity.getId())) {
            throw new IllegalArgumentException("不能添加自己为好友");
        }
        enforceApplyRateLimit(userEntity.getAccount(), friendEntity.getAccount());

        if (userRelationRepository.existsByUserAndFriendAndRelationType(userEntity, friendEntity, UserRelationEnum.BLOCKED)) {
            throw new IllegalStateException("你已拉黑该用户");
        }
        if (userRelationRepository.existsByUserAndFriendAndRelationType(friendEntity, userEntity, UserRelationEnum.BLOCKED)) {
            throw new IllegalStateException("已被对方拉黑");
        }

        UserRelation outbound = userRelationRepository.findByUserAndFriend(userEntity, friendEntity).orElse(null);
        UserRelation inbound = userRelationRepository.findByUserAndFriend(friendEntity, userEntity).orElse(null);
        expireIfNeeded(outbound);
        expireIfNeeded(inbound);

        if (hasAccepted(outbound) || hasAccepted(inbound)) {
            throw new IllegalStateException("已经是好友");
        }

        if (isPending(outbound)) {
            throw new IllegalStateException("好友申请已存在");
        }

        if (isPending(inbound)) {
            ensureRequestId(inbound);
            inbound.setRelationType(UserRelationEnum.ACCEPTED);
            inbound.setSource(SOURCE_ACCOUNT_SEARCH);
            inbound.setExpiredAt(null);
            inbound.setOperatorAccount(userEntity.getAccount());
            UserRelation saved = userRelationRepository.save(inbound);
            publishFriendRequestEvent(saved, FriendRequestEventType.FRIEND_REQUEST_ACCEPTED,
                    Set.of(saved.getUser().getAccount(), saved.getFriend().getAccount()));
            publishAcceptedGreeting(userEntity.getAccount(), friendEntity.getAccount());
            return toActionResult(saved);
        }

        UserRelation relation = saveOrUpdateRelation(
                userEntity,
                friendEntity,
                UserRelationEnum.PENDING,
                normalizeVerificationMessage(verificationMessage),
                SOURCE_ACCOUNT_SEARCH,
                userEntity.getAccount(),
                Instant.now().plusSeconds(7L * 24 * 3600),
                null
        );
        publishFriendRequestEvent(relation, FriendRequestEventType.FRIEND_REQUEST_CREATED, Set.of(relation.getFriend().getAccount()));
        return toActionResult(relation);
    }

    @Override
    @Transactional
    public void removeRelation(UserEntity userEntity, UserEntity friendEntity) {
        if (userEntity.getId().equals(friendEntity.getId())) {
            throw new IllegalArgumentException("不能删除自己");
        }

        List<UserRelation> pairRelations = userRelationRepository.findPairRelations(userEntity, friendEntity);
        if (pairRelations.isEmpty()) {
            throw new IllegalStateException("好友关系不存在");
        }
        userRelationRepository.deleteAllInBatch(pairRelations);
    }

    @Override
    @Transactional
    public FriendRequestActionResultVO acceptRelation(UserEntity userEntity, UserEntity friendEntity) {
        if (userEntity.getId().equals(friendEntity.getId())) {
            throw new IllegalArgumentException("不能同意自己的好友申请");
        }

        UserRelation relation = userRelationRepository
                .findByUserAndFriend(friendEntity, userEntity)
                .orElseThrow(() -> new IllegalStateException("好友请求不存在"));

        expireIfNeeded(relation);
        if (relation.getRelationType() == UserRelationEnum.EXPIRED) {
            throw new IllegalStateException("好友请求已过期");
        }
        if (relation.getRelationType() == UserRelationEnum.ACCEPTED) {
            throw new IllegalStateException("好友请求已经同意");
        }
        if (relation.getRelationType() == UserRelationEnum.REJECTED
                || relation.getRelationType() == UserRelationEnum.CANCELED
                || relation.getRelationType() == UserRelationEnum.BLOCKED) {
            throw new IllegalStateException("好友请求状态冲突，无法同意");
        }

        relation.setRelationType(UserRelationEnum.ACCEPTED);
        ensureRequestId(relation);
        relation.setExpiredAt(null);
        relation.setOperatorAccount(userEntity.getAccount());
        relation.setSource(SOURCE_ACCOUNT_SEARCH);
        UserRelation saved = userRelationRepository.save(relation);
        publishFriendRequestEvent(saved, FriendRequestEventType.FRIEND_REQUEST_ACCEPTED,
                Set.of(saved.getUser().getAccount(), saved.getFriend().getAccount()));
        publishAcceptedGreeting(userEntity.getAccount(), friendEntity.getAccount());
        return toActionResult(saved);
    }

    @Override
    @Transactional
    public FriendRequestActionResultVO rejectRelation(UserEntity userEntity, UserEntity friendEntity) {
        UserRelation relation = userRelationRepository
                .findByUserAndFriend(friendEntity, userEntity)
                .orElseThrow(() -> new IllegalStateException("好友请求不存在"));

        expireIfNeeded(relation);
        if (relation.getRelationType() == UserRelationEnum.EXPIRED) {
            throw new IllegalStateException("好友请求已过期");
        }
        if (relation.getRelationType() == UserRelationEnum.REJECTED) {
            throw new IllegalStateException("好友请求已经拒绝");
        }
        if (relation.getRelationType() == UserRelationEnum.ACCEPTED
                || relation.getRelationType() == UserRelationEnum.CANCELED
                || relation.getRelationType() == UserRelationEnum.BLOCKED) {
            throw new IllegalStateException("好友请求状态冲突，无法拒绝");
        }

        relation.setRelationType(UserRelationEnum.REJECTED);
        ensureRequestId(relation);
        relation.setOperatorAccount(userEntity.getAccount());
        relation.setExpiredAt(null);
        UserRelation saved = userRelationRepository.save(relation);
        publishFriendRequestEvent(saved, FriendRequestEventType.FRIEND_REQUEST_REJECTED,
                Set.of(saved.getUser().getAccount(), saved.getFriend().getAccount()));
        return toActionResult(saved);
    }

    @Override
    @Transactional
    public FriendRequestActionResultVO cancelRelation(UserEntity userEntity, String requestId) {
        UserRelation relation = userRelationRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalStateException("好友请求不存在"));

        if (!relation.getUser().getId().equals(userEntity.getId())) {
            throw new IllegalStateException("无权限取消该好友请求");
        }

        expireIfNeeded(relation);
        if (relation.getRelationType() == UserRelationEnum.CANCELED) {
            throw new IllegalStateException("好友请求已经取消");
        }
        if (relation.getRelationType() == UserRelationEnum.EXPIRED) {
            throw new IllegalStateException("好友请求已过期");
        }
        if (relation.getRelationType() == UserRelationEnum.ACCEPTED
                || relation.getRelationType() == UserRelationEnum.REJECTED
                || relation.getRelationType() == UserRelationEnum.BLOCKED) {
            throw new IllegalStateException("好友请求状态冲突，无法取消");
        }

        relation.setRelationType(UserRelationEnum.CANCELED);
        ensureRequestId(relation);
        relation.setOperatorAccount(userEntity.getAccount());
        relation.setExpiredAt(null);
        UserRelation saved = userRelationRepository.save(relation);
        publishFriendRequestEvent(saved, FriendRequestEventType.FRIEND_REQUEST_CANCELED,
                Set.of(saved.getUser().getAccount(), saved.getFriend().getAccount()));
        return toActionResult(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserRelation> listFriendRelations(UserEntity userEntity) {
        return userRelationRepository.findAllByOwnerAndRelationType(userEntity, UserRelationEnum.ACCEPTED).stream()
                .filter(relation -> !SOURCE_SYSTEM_MIRROR.equalsIgnoreCase(defaultString(relation.getSource())))
                .collect(java.util.stream.Collectors.toMap(
                        relation -> counterpartId(relation, userEntity),
                        relation -> relation,
                        this::keepNewestRelation
                ))
                .values()
                .stream()
                .toList();
    }

    @Override
    @Transactional
    public List<UserRelation> listPendingRequests(UserEntity userEntity) {
        List<UserRelation> relations = userRelationRepository.findByFriendAndRelationType(userEntity, UserRelationEnum.PENDING);
        List<UserRelation> activePending = new ArrayList<>();
        for (UserRelation relation : relations) {
            if (!expireIfNeeded(relation)) {
                activePending.add(relation);
            }
        }
        return activePending;
    }

    @Override
    @Transactional
    public PageResultVO<FriendRequestHistoryItemVO> queryRequestHistory(
            UserEntity currentUser,
            int page,
            int size,
            String direction,
            Set<String> statuses,
            Instant startTime,
            Instant endTime,
            String keyword
    ) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("createTime")));
        Page<UserRelation> relationPage = userRelationRepository.findAll(buildHistorySpec(
                currentUser,
                direction,
                statuses,
                startTime,
                endTime,
                keyword
        ), pageable);

        List<FriendRequestHistoryItemVO> items = relationPage.getContent().stream()
                .map(relation -> {
                    expireIfNeeded(relation);
                    return toHistoryItem(relation, currentUser);
                })
                .toList();
        items = deduplicateHistory(items);

        return PageResultVO.<FriendRequestHistoryItemVO>builder()
                .records(items)
                .page(page)
                .size(size)
                .total(relationPage.getTotalElements())
                .totalPages(relationPage.getTotalPages())
                .hasMore(page < relationPage.getTotalPages())
                .build();
    }

    @Override
    @Transactional
    public FriendRequestHistoryItemVO queryRequestHistoryDetail(UserEntity currentUser, String requestId) {
        UserRelation relation = userRelationRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalStateException("好友请求不存在"));

        boolean isInbound = relation.getFriend().getId().equals(currentUser.getId());
        boolean isOutbound = relation.getUser().getId().equals(currentUser.getId());
        if (!isInbound && !isOutbound) {
            throw new IllegalStateException("无权限查看该好友请求");
        }

        expireIfNeeded(relation);
        return toHistoryItem(relation, currentUser);
    }

    private Specification<UserRelation> buildHistorySpec(
            UserEntity currentUser,
            String direction,
            Set<String> statuses,
            Instant startTime,
            Instant endTime,
            String keyword
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Predicate ownerPredicate = cb.or(
                    cb.equal(root.get("user").get("id"), currentUser.getId()),
                    cb.equal(root.get("friend").get("id"), currentUser.getId())
            );
            predicates.add(ownerPredicate);
            predicates.add(cb.or(
                    cb.isNull(root.get("source")),
                    cb.notEqual(cb.lower(root.get("source")), SOURCE_SYSTEM_MIRROR.toLowerCase(Locale.ROOT))
            ));

            if ("INBOUND".equalsIgnoreCase(direction)) {
                predicates.add(cb.equal(root.get("friend").get("id"), currentUser.getId()));
            } else if ("OUTBOUND".equalsIgnoreCase(direction)) {
                predicates.add(cb.equal(root.get("user").get("id"), currentUser.getId()));
            }

            if (statuses != null && !statuses.isEmpty()) {
                List<UserRelationEnum> statusEnums = statuses.stream()
                        .map(s -> UserRelationEnum.valueOf(s.toUpperCase(Locale.ROOT)))
                        .toList();
                predicates.add(root.get("relationType").in(statusEnums));
            }

            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createTime"), toUtcLocalDateTime(startTime)));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createTime"), toUtcLocalDateTime(endTime)));
            }

            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                Predicate byApplicantAccount = cb.like(cb.lower(root.get("user").get("account")), like);
                Predicate byApplicantName = cb.like(cb.lower(root.get("user").get("realName")), like);
                Predicate byTargetAccount = cb.like(cb.lower(root.get("friend").get("account")), like);
                Predicate byTargetName = cb.like(cb.lower(root.get("friend").get("realName")), like);
                predicates.add(cb.or(byApplicantAccount, byApplicantName, byTargetAccount, byTargetName));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private UserRelation saveOrUpdateRelation(
            UserEntity userEntity,
            UserEntity friendEntity,
            UserRelationEnum relationType,
            String verificationMessage,
            String source,
            String operatorAccount,
            Instant expiredAt,
            String requestId
    ) {
        UserRelation relation = userRelationRepository.findByUserAndFriend(userEntity, friendEntity)
                .orElseGet(UserRelation::new);
        relation.setUser(userEntity);
        relation.setFriend(friendEntity);
        relation.setRelationType(relationType);
        relation.setVerificationMessage(verificationMessage);
        relation.setSource(source);
        relation.setOperatorAccount(operatorAccount);
        relation.setExpiredAt(expiredAt);
        relation.setRequestId(requestId == null || requestId.isBlank() ? generateRequestId() : requestId);
        return userRelationRepository.save(relation);
    }

    private boolean hasAccepted(UserRelation relation) {
        return relation != null && relation.getRelationType() == UserRelationEnum.ACCEPTED;
    }

    private boolean isPending(UserRelation relation) {
        return relation != null && relation.getRelationType() == UserRelationEnum.PENDING;
    }

    private boolean expireIfNeeded(UserRelation relation) {
        if (relation == null || relation.getRelationType() != UserRelationEnum.PENDING || relation.getExpiredAt() == null) {
            return false;
        }
        if (relation.getExpiredAt().isAfter(Instant.now())) {
            return false;
        }

        relation.setRelationType(UserRelationEnum.EXPIRED);
        relation.setOperatorAccount(OPERATOR_SYSTEM);
        UserRelation saved = userRelationRepository.save(relation);
        publishFriendRequestEvent(saved, FriendRequestEventType.FRIEND_REQUEST_EXPIRED,
                Set.of(saved.getUser().getAccount(), saved.getFriend().getAccount()));
        return true;
    }

    private void publishFriendRequestEvent(UserRelation relation,
                                           FriendRequestEventType eventType,
                                           Set<String> targetAccounts) {
        String requestId = resolveRequestId(relation);
        Instant occurredAt = Instant.now();
        for (String targetAccount : targetAccounts) {
            FriendRequestEventVO payload = FriendRequestEventVO.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(eventType)
                    .occurredAt(occurredAt)
                    .requestId(requestId)
                    .fromAccount(relation.getUser().getAccount())
                    .fromName(relation.getUser().getRealName())
                    .fromAvatarUrl(relation.getUser().getAvatarUrl())
                    .toAccount(relation.getFriend().getAccount())
                    .status(relation.getRelationType())
                    .unreadPendingCount(unreadPendingCountFor(targetAccount, relation))
                    .build();
            eventPublisher.publishEvent(new FriendRequestWsDomainEvent(targetAccount, payload));
        }
    }

    private void publishAcceptedGreeting(String fromAccount, String toAccount) {
        if (fromAccount == null || fromAccount.isBlank() || toAccount == null || toAccount.isBlank()) {
            return;
        }
        eventPublisher.publishEvent(new FriendAcceptedGreetingDomainEvent(
                fromAccount,
                toAccount,
                FRIEND_ACCEPTED_GREETING
        ));
    }

    private long unreadPendingCountFor(String targetAccount, UserRelation relation) {
        if (targetAccount == null) {
            return 0;
        }
        if (targetAccount.equals(relation.getFriend().getAccount())) {
            return userRelationRepository.countByFriendAndRelationType(relation.getFriend(), UserRelationEnum.PENDING);
        }
        if (targetAccount.equals(relation.getUser().getAccount())) {
            return userRelationRepository.countByFriendAndRelationType(relation.getUser(), UserRelationEnum.PENDING);
        }
        return 0;
    }

    private void enforceApplyRateLimit(String fromAccount, String toAccount) {
        String key = fromAccount + "->" + toAccount;
        Instant now = Instant.now();
        Instant previous = APPLY_RATE_LIMIT_CACHE.put(key, now);
        if (previous == null) {
            return;
        }
        if (Duration.between(previous, now).compareTo(APPLY_RATE_LIMIT_WINDOW) < 0) {
            throw new IllegalStateException("好友申请过于频繁，请稍后再试");
        }
    }

    private FriendRequestHistoryItemVO toHistoryItem(UserRelation relation, UserEntity currentUser) {
        String resolvedRequestId = resolveRequestId(relation);
        String resolvedOperatorAccount = resolveOperatorAccount(relation);
        FriendRequestDirectionEnum direction = relation.getFriend().getId().equals(currentUser.getId())
                ? FriendRequestDirectionEnum.INBOUND
                : FriendRequestDirectionEnum.OUTBOUND;

        return FriendRequestHistoryItemVO.builder()
                .requestId(resolvedRequestId)
                .direction(direction)
                .status(relation.getRelationType())
                .applicantAccount(relation.getUser().getAccount())
                .applicantName(relation.getUser().getRealName())
                .applicantAvatarUrl(relation.getUser().getAvatarUrl())
                .applicantSignature(relation.getUser().getSignature())
                .targetAccount(relation.getFriend().getAccount())
                .targetName(relation.getFriend().getRealName())
                .targetAvatarUrl(relation.getFriend().getAvatarUrl())
                .targetSignature(relation.getFriend().getSignature())
                .verificationMessage(relation.getVerificationMessage())
                .source(defaultString(relation.getSource()))
                .operatorAccount(resolvedOperatorAccount)
                .createdAt(toUtcOffsetDateTime(relation.getCreateTime()))
                .updatedAt(toUtcOffsetDateTime(relation.getUpdatedAt() == null ? relation.getCreateTime() : relation.getUpdatedAt()))
                .expiredAt(relation.getExpiredAt() == null ? null : OffsetDateTime.ofInstant(relation.getExpiredAt(), ZoneOffset.UTC))
                .build();
    }

    private FriendRequestActionResultVO toActionResult(UserRelation relation) {
        String resolvedRequestId = resolveRequestId(relation);
        String resolvedOperatorAccount = resolveOperatorAccount(relation);
        return FriendRequestActionResultVO.builder()
                .requestId(resolvedRequestId)
                .status(relation.getRelationType())
                .operatorAccount(resolvedOperatorAccount)
                .updatedAt(toUtcOffsetDateTime(relation.getUpdatedAt() == null ? relation.getCreateTime() : relation.getUpdatedAt()))
                .expiredAt(relation.getExpiredAt() == null ? null : OffsetDateTime.ofInstant(relation.getExpiredAt(), ZoneOffset.UTC))
                .build();
    }

    private OffsetDateTime toUtcOffsetDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atOffset(ZoneOffset.UTC);
    }

    private LocalDateTime toUtcLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private String normalizeVerificationMessage(String verificationMessage) {
        if (verificationMessage == null) {
            return null;
        }
        String normalized = verificationMessage.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > VERIFY_MESSAGE_MAX_LEN) {
            throw new IllegalArgumentException("verificationMessage 不能超过 200 字");
        }

        return normalized
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String generateRequestId() {
        return "fr_" + Instant.now().toEpochMilli() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private Long counterpartId(UserRelation relation, UserEntity currentUser) {
        return relation.getUser().getId().equals(currentUser.getId())
                ? relation.getFriend().getId()
                : relation.getUser().getId();
    }

    private UserRelation keepNewestRelation(UserRelation left, UserRelation right) {
        LocalDateTime leftTime = left.getUpdatedAt() == null ? left.getCreateTime() : left.getUpdatedAt();
        LocalDateTime rightTime = right.getUpdatedAt() == null ? right.getCreateTime() : right.getUpdatedAt();
        if (leftTime == null) {
            return right;
        }
        if (rightTime == null) {
            return left;
        }
        return leftTime.isAfter(rightTime) ? left : right;
    }

    private List<FriendRequestHistoryItemVO> deduplicateHistory(List<FriendRequestHistoryItemVO> rawItems) {
        // Normal records: keep by requestId only (no business de-duplication).
        Map<String, FriendRequestHistoryItemVO> normalByRequestId = new LinkedHashMap<>();
        List<FriendRequestHistoryItemVO> legacyItems = new ArrayList<>();
        for (FriendRequestHistoryItemVO item : rawItems) {
            if (isLegacyRequestId(item.getRequestId())) {
                legacyItems.add(item);
                continue;
            }
            FriendRequestHistoryItemVO existing = normalByRequestId.get(item.getRequestId());
            if (existing == null || isAfter(item.getUpdatedAt(), existing.getUpdatedAt())) {
                normalByRequestId.put(item.getRequestId(), item);
            }
        }

        // Legacy records: apply strong de-duplication for historical mirrored/dirty data only.
        List<FriendRequestHistoryItemVO> sortedLegacy = legacyItems.stream()
                .sorted(Comparator.comparing(FriendRequestHistoryItemVO::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        Map<String, FriendRequestHistoryItemVO> byExact = new LinkedHashMap<>();
        for (FriendRequestHistoryItemVO item : sortedLegacy) {
            String key = exactKey(item);
            FriendRequestHistoryItemVO existing = byExact.get(key);
            if (existing == null || isAfter(item.getUpdatedAt(), existing.getUpdatedAt())) {
                byExact.put(key, item);
            }
        }

        List<FriendRequestHistoryItemVO> stageOne = new ArrayList<>(byExact.values());
        List<FriendRequestHistoryItemVO> result = new ArrayList<>();
        boolean[] used = new boolean[stageOne.size()];
        for (int i = 0; i < stageOne.size(); i++) {
            if (used[i]) {
                continue;
            }
            FriendRequestHistoryItemVO current = stageOne.get(i);
            int mirrorIndex = findMirrorIndex(stageOne, used, i);
            if (mirrorIndex < 0) {
                result.add(current);
                used[i] = true;
                continue;
            }

            FriendRequestHistoryItemVO mirror = stageOne.get(mirrorIndex);
            used[i] = true;
            used[mirrorIndex] = true;
            result.add(pickEarlierCreated(current, mirror));
        }

        List<FriendRequestHistoryItemVO> merged = new ArrayList<>(normalByRequestId.values());
        merged.addAll(result);
        return merged.stream()
                .sorted(Comparator.comparing(FriendRequestHistoryItemVO::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private String exactKey(FriendRequestHistoryItemVO item) {
        return defaultString(item.getApplicantAccount()) + "|" +
                defaultString(item.getTargetAccount()) + "|" +
                String.valueOf(item.getCreatedAt());
    }

    private int findMirrorIndex(List<FriendRequestHistoryItemVO> items, boolean[] used, int currentIndex) {
        FriendRequestHistoryItemVO current = items.get(currentIndex);
        for (int i = currentIndex + 1; i < items.size(); i++) {
            if (used[i]) {
                continue;
            }
            FriendRequestHistoryItemVO candidate = items.get(i);
            if (!isMirror(current, candidate)) {
                continue;
            }
            if (!isCloseTime(current.getCreatedAt(), candidate.getCreatedAt())) {
                continue;
            }
            return i;
        }
        return -1;
    }

    private boolean isMirror(FriendRequestHistoryItemVO left, FriendRequestHistoryItemVO right) {
        return Objects.equals(left.getApplicantAccount(), right.getTargetAccount())
                && Objects.equals(left.getTargetAccount(), right.getApplicantAccount());
    }

    private boolean isCloseTime(OffsetDateTime left, OffsetDateTime right) {
        if (left == null || right == null) {
            return false;
        }
        long seconds = Math.abs(Duration.between(left, right).getSeconds());
        return seconds <= 120;
    }

    private FriendRequestHistoryItemVO pickEarlierCreated(FriendRequestHistoryItemVO left, FriendRequestHistoryItemVO right) {
        if (left.getCreatedAt() == null) {
            return right;
        }
        if (right.getCreatedAt() == null) {
            return left;
        }
        return left.getCreatedAt().isBefore(right.getCreatedAt()) ? left : right;
    }

    private boolean isAfter(OffsetDateTime current, OffsetDateTime baseline) {
        if (current == null) {
            return false;
        }
        if (baseline == null) {
            return true;
        }
        return current.isAfter(baseline);
    }

    private boolean isLegacyRequestId(String requestId) {
        return requestId == null || requestId.isBlank() || requestId.startsWith("legacy_");
    }

    private void ensureRequestId(UserRelation relation) {
        if (relation.getRequestId() == null || relation.getRequestId().isBlank()) {
            relation.setRequestId(generateRequestId());
        }
    }

    private String resolveRequestId(UserRelation relation) {
        if (relation.getRequestId() != null && !relation.getRequestId().isBlank()) {
            return relation.getRequestId();
        }
        return "legacy_" + relation.getId();
    }

    private String resolveOperatorAccount(UserRelation relation) {
        if (relation.getOperatorAccount() != null && !relation.getOperatorAccount().isBlank()) {
            return relation.getOperatorAccount();
        }
        return switch (relation.getRelationType()) {
            case ACCEPTED, REJECTED -> relation.getFriend().getAccount();
            case CANCELED -> relation.getUser().getAccount();
            case EXPIRED -> OPERATOR_SYSTEM;
            default -> null;
        };
    }
}
