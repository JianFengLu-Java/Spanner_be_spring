package com.lujianfeng.spanner.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lujianfeng.spanner.dto.task.TaskEventRequestDTO;
import com.lujianfeng.spanner.entity.task.DailyCounterEntity;
import com.lujianfeng.spanner.entity.task.GrowthAccountEntity;
import com.lujianfeng.spanner.entity.task.GrowthLedgerEntity;
import com.lujianfeng.spanner.entity.task.RewardGrantEntity;
import com.lujianfeng.spanner.entity.task.TaskConfigEntity;
import com.lujianfeng.spanner.entity.task.TaskEventEntity;
import com.lujianfeng.spanner.entity.task.WalletLedgerEntity;
import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.entity.user.WalletAccountEntity;
import com.lujianfeng.spanner.entity.user.WalletFlowEntity;
import com.lujianfeng.spanner.repository.DailyCounterRepository;
import com.lujianfeng.spanner.repository.GrowthAccountRepository;
import com.lujianfeng.spanner.repository.GrowthLedgerRepository;
import com.lujianfeng.spanner.repository.RewardGrantRepository;
import com.lujianfeng.spanner.repository.TaskConfigRepository;
import com.lujianfeng.spanner.repository.TaskEventRepository;
import com.lujianfeng.spanner.repository.UserRepository;
import com.lujianfeng.spanner.repository.WalletAccountRepository;
import com.lujianfeng.spanner.repository.WalletFlowRepository;
import com.lujianfeng.spanner.repository.WalletLedgerRepository;
import com.lujianfeng.spanner.service.task.TaskRewardService;
import com.lujianfeng.spanner.vo.task.GrowthLedgerItemVO;
import com.lujianfeng.spanner.vo.task.TaskConfigVO;
import com.lujianfeng.spanner.vo.task.TaskEventProcessResultVO;
import com.lujianfeng.spanner.vo.task.TodayRewardProgressVO;
import com.lujianfeng.spanner.vo.task.WalletLedgerItemVO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaskRewardServiceImpl implements TaskRewardService {

    private static final String POST_CREATE = "POST_CREATE";
    private static final String REPLY_CREATE = "REPLY_CREATE";

    private final TaskConfigRepository taskConfigRepository;
    private final TaskEventRepository taskEventRepository;
    private final RewardGrantRepository rewardGrantRepository;
    private final DailyCounterRepository dailyCounterRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final WalletFlowRepository walletFlowRepository;
    private final WalletLedgerRepository walletLedgerRepository;
    private final GrowthAccountRepository growthAccountRepository;
    private final GrowthLedgerRepository growthLedgerRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TaskRewardServiceImpl(TaskConfigRepository taskConfigRepository,
                                 TaskEventRepository taskEventRepository,
                                 RewardGrantRepository rewardGrantRepository,
                                 DailyCounterRepository dailyCounterRepository,
                                 WalletAccountRepository walletAccountRepository,
                                 WalletFlowRepository walletFlowRepository,
                                 WalletLedgerRepository walletLedgerRepository,
                                 GrowthAccountRepository growthAccountRepository,
                                 GrowthLedgerRepository growthLedgerRepository,
                                 UserRepository userRepository) {
        this.taskConfigRepository = taskConfigRepository;
        this.taskEventRepository = taskEventRepository;
        this.rewardGrantRepository = rewardGrantRepository;
        this.dailyCounterRepository = dailyCounterRepository;
        this.walletAccountRepository = walletAccountRepository;
        this.walletFlowRepository = walletFlowRepository;
        this.walletLedgerRepository = walletLedgerRepository;
        this.growthAccountRepository = growthAccountRepository;
        this.growthLedgerRepository = growthLedgerRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public TaskEventProcessResultVO handleTaskEvent(TaskEventRequestDTO request) {
        validateRequest(request);
        initDefaultTaskConfigIfAbsent();

        Optional<TaskEventEntity> existingEvent = taskEventRepository.findByEventId(request.getEventId());
        if (existingEvent.isPresent()) {
            return buildDuplicateResult(existingEvent.get());
        }

        TaskConfigEntity config = taskConfigRepository.findByTaskType(request.getEventType())
                .orElseThrow(() -> new IllegalArgumentException("任务类型不存在"));

        TaskEventEntity event = buildTaskEvent(request);
        try {
            taskEventRepository.save(event);
        } catch (DataIntegrityViolationException ex) {
            TaskEventEntity duplicated = taskEventRepository.findByEventId(request.getEventId())
                    .orElseThrow(() -> ex);
            return buildDuplicateResult(duplicated);
        }

        if (Boolean.FALSE.equals(config.getEnabled())) {
            return markSkipped(event, config, "SKIPPED_DISABLED", "TASK_DISABLED", 0, 0, null, null);
        }

        String invalidReason = validateEventState(request);
        if (invalidReason != null) {
            return markSkipped(event, config, "SKIPPED_INVALID", invalidReason, 0, 0, null, null);
        }

        String riskReason = evaluateRisk(request, config);
        if (riskReason != null) {
            return markSkipped(event, config, "SKIPPED_RISK", riskReason, 0, 0, null, null);
        }

        if (POST_CREATE.equals(request.getEventType())) {
            return grantPostCreate(request, event, config);
        }
        if (REPLY_CREATE.equals(request.getEventType())) {
            return grantReplyCreate(request, event, config);
        }
        return markSkipped(event, config, "SKIPPED_INVALID", "UNSUPPORTED_EVENT", 0, 0, null, null);
    }

    @Override
    @Transactional
    public List<TaskConfigVO> listTaskConfig() {
        initDefaultTaskConfigIfAbsent();
        return taskConfigRepository.findAll().stream()
                .map(c -> TaskConfigVO.builder()
                        .taskType(c.getTaskType())
                        .enabled(c.getEnabled())
                        .rewardWalletCent(c.getRewardWalletCent())
                        .rewardGrowth(c.getRewardGrowth())
                        .dailyLimit(c.getDailyLimit())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public TodayRewardProgressVO getTodayProgress(Long userId) {
        initDefaultTaskConfigIfAbsent();
        LocalDate today = LocalDate.now();
        TaskConfigEntity config = taskConfigRepository.findByTaskType(POST_CREATE).orElse(null);
        int limit = config == null || config.getDailyLimit() == null ? 0 : config.getDailyLimit();
        int granted = dailyCounterRepository.findByUserIdAndTaskTypeAndBizDate(userId, POST_CREATE, today)
                .map(DailyCounterEntity::getGrantedCount)
                .orElse(0);
        int remain = Math.max(0, limit - granted);
        return TodayRewardProgressVO.builder()
                .userId(userId)
                .date(today.toString())
                .timezone(ZoneId.systemDefault().getId())
                .postGrantedCount(granted)
                .postDailyLimit(limit)
                .postRemainingCount(remain)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WalletLedgerItemVO> getWalletLedger(Long userId, int page, int size) {
        return walletLedgerRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(item -> WalletLedgerItemVO.builder()
                        .ledgerId(item.getLedgerId())
                        .bizType(item.getBizType())
                        .taskType(item.getTaskType())
                        .bizId(item.getBizId())
                        .changeCent(item.getChangeCent())
                        .balanceAfterCent(item.getBalanceAfterCent())
                        .createdAt(item.getCreatedAt())
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GrowthLedgerItemVO> getGrowthLedger(Long userId, int page, int size) {
        return growthLedgerRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(item -> GrowthLedgerItemVO.builder()
                        .ledgerId(item.getLedgerId())
                        .bizType(item.getBizType())
                        .taskType(item.getTaskType())
                        .bizId(item.getBizId())
                        .changeGrowth(item.getChangeGrowth())
                        .growthAfter(item.getGrowthAfter())
                        .createdAt(item.getCreatedAt())
                        .build());
    }

    @Override
    public void onMomentCreated(String momentId, Long actorUserId) {
        TaskEventRequestDTO dto = new TaskEventRequestDTO();
        dto.setEventId(UUID.randomUUID().toString());
        dto.setEventType(POST_CREATE);
        dto.setBizId(momentId);
        dto.setActorUserId(actorUserId);
        dto.setCreatedAt(Instant.now());
        dto.setMeta(Map.of(
                "isDraft", false,
                "isDeleted", false,
                "isBlocked", false
        ));
        handleTaskEvent(dto);
    }

    @Override
    public void onCommentCreated(String commentId, String momentId, Long actorUserId, String content) {
        TaskEventRequestDTO dto = new TaskEventRequestDTO();
        dto.setEventId(UUID.randomUUID().toString());
        dto.setEventType(REPLY_CREATE);
        dto.setBizId(commentId);
        dto.setTargetId(momentId);
        dto.setActorUserId(actorUserId);
        dto.setCreatedAt(Instant.now());
        dto.setMeta(Map.of(
                "isDeleted", false,
                "isBlocked", false,
                "isSystemBackfill", false,
                "contentLength", content == null ? 0 : content.length()
        ));
        handleTaskEvent(dto);
    }

    private TaskEventProcessResultVO grantPostCreate(TaskEventRequestDTO request, TaskEventEntity event, TaskConfigEntity config) {
        LocalDate today = LocalDate.now();
        int dailyLimit = config.getDailyLimit() == null ? 0 : config.getDailyLimit();
        DailyCounterEntity counter = lockOrCreateCounter(request.getActorUserId(), POST_CREATE, today);

        if (counter.getGrantedCount() >= dailyLimit) {
            return markSkipped(event, config, "SKIPPED_LIMIT", "DAILY_LIMIT_REACHED", 0, 0,
                    counter.getGrantedCount(), Math.max(0, dailyLimit - counter.getGrantedCount()));
        }

        WalletAccountEntity wallet = walletAccountRepository.findByUserIdForUpdate(request.getActorUserId());
        if (wallet == null) {
            UserEntity user = userRepository.findById(request.getActorUserId())
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
            wallet = createWalletForUser(user);
            wallet = walletAccountRepository.findByUserIdForUpdate(request.getActorUserId());
        }

        long beforeCent = wallet.getBalanceCent() == null ? decimalToCent(wallet.getBalance()) : wallet.getBalanceCent();
        long afterCent = beforeCent + config.getRewardWalletCent();
        wallet.setBalanceCent(afterCent);
        wallet.setRewardVersion((wallet.getRewardVersion() == null ? 0 : wallet.getRewardVersion()) + 1);
        wallet.setBalance(BigDecimal.valueOf(afterCent, 2));
        walletAccountRepository.save(wallet);

        WalletLedgerEntity walletLedger = new WalletLedgerEntity();
        walletLedger.setLedgerId("wl_" + request.getEventId());
        walletLedger.setUserId(request.getActorUserId());
        walletLedger.setChangeCent(config.getRewardWalletCent());
        walletLedger.setBalanceAfterCent(afterCent);
        walletLedger.setBizType("TASK_REWARD");
        walletLedger.setTaskType(POST_CREATE);
        walletLedger.setEventId(request.getEventId());
        walletLedger.setBizId(request.getBizId());
        walletLedgerRepository.save(walletLedger);

        WalletFlowEntity flow = new WalletFlowEntity();
        flow.setWalletId(wallet.getId());
        flow.setWalletNo(wallet.getWalletNo());
        flow.setUserId(wallet.getUserId());
        flow.setBusinessNo("TASK_REWARD_" + request.getEventId());
        flow.setChangeType("REWARD");
        flow.setAmount(BigDecimal.valueOf(config.getRewardWalletCent(), 2));
        flow.setBeforeBalance(BigDecimal.valueOf(beforeCent, 2));
        flow.setAfterBalance(BigDecimal.valueOf(afterCent, 2));
        flow.setRemark("任务奖励: " + POST_CREATE + ", bizId=" + request.getBizId());
        walletFlowRepository.save(flow);

        counter.setGrantedCount(counter.getGrantedCount() + 1);
        dailyCounterRepository.save(counter);

        RewardGrantEntity grant = new RewardGrantEntity();
        grant.setGrantId("g_" + request.getEventId());
        grant.setEventId(request.getEventId());
        grant.setUserId(request.getActorUserId());
        grant.setTaskType(POST_CREATE);
        grant.setWalletCent(config.getRewardWalletCent());
        grant.setGrowth(0);
        grant.setStatus("GRANTED");
        rewardGrantRepository.save(grant);

        event.setProcessStatus("GRANTED");
        event.setProcessReason("OK");
        taskEventRepository.save(event);

        int grantedCount = counter.getGrantedCount();
        int remaining = Math.max(0, dailyLimit - grantedCount);
        return TaskEventProcessResultVO.builder()
                .eventId(request.getEventId())
                .taskType(POST_CREATE)
                .grantStatus("GRANTED")
                .rewardWalletCent(config.getRewardWalletCent())
                .rewardGrowth(0)
                .todayGrantedCount(grantedCount)
                .todayRemainingCount(remaining)
                .reason("OK")
                .duplicate(false)
                .build();
    }

    private TaskEventProcessResultVO grantReplyCreate(TaskEventRequestDTO request, TaskEventEntity event, TaskConfigEntity config) {
        GrowthAccountEntity account = growthAccountRepository.findByUserIdForUpdate(request.getActorUserId())
                .orElseGet(() -> {
                    GrowthAccountEntity created = new GrowthAccountEntity();
                    created.setUserId(request.getActorUserId());
                    created.setGrowthValue(0L);
                    created.setVersion(0);
                    return growthAccountRepository.save(created);
                });
        if (account.getId() == null) {
            account = growthAccountRepository.findByUserIdForUpdate(request.getActorUserId()).orElse(account);
        }

        long after = (account.getGrowthValue() == null ? 0L : account.getGrowthValue()) + config.getRewardGrowth();
        account.setGrowthValue(after);
        account.setVersion((account.getVersion() == null ? 0 : account.getVersion()) + 1);
        growthAccountRepository.save(account);

        UserEntity user = userRepository.findByIdForUpdate(request.getActorUserId());
        if (user != null) {
            long base = user.getGrowthValue() == null ? 0L : user.getGrowthValue();
            user.setGrowthValue(base + config.getRewardGrowth());
            userRepository.save(user);
        }

        GrowthLedgerEntity growthLedger = new GrowthLedgerEntity();
        growthLedger.setLedgerId("gl_" + request.getEventId());
        growthLedger.setUserId(request.getActorUserId());
        growthLedger.setChangeGrowth(config.getRewardGrowth());
        growthLedger.setGrowthAfter(after);
        growthLedger.setBizType("TASK_REWARD");
        growthLedger.setTaskType(REPLY_CREATE);
        growthLedger.setEventId(request.getEventId());
        growthLedger.setBizId(request.getBizId());
        growthLedgerRepository.save(growthLedger);

        RewardGrantEntity grant = new RewardGrantEntity();
        grant.setGrantId("g_" + request.getEventId());
        grant.setEventId(request.getEventId());
        grant.setUserId(request.getActorUserId());
        grant.setTaskType(REPLY_CREATE);
        grant.setWalletCent(0);
        grant.setGrowth(config.getRewardGrowth());
        grant.setStatus("GRANTED");
        rewardGrantRepository.save(grant);

        event.setProcessStatus("GRANTED");
        event.setProcessReason("OK");
        taskEventRepository.save(event);

        return TaskEventProcessResultVO.builder()
                .eventId(request.getEventId())
                .taskType(REPLY_CREATE)
                .grantStatus("GRANTED")
                .rewardWalletCent(0)
                .rewardGrowth(config.getRewardGrowth())
                .reason("OK")
                .duplicate(false)
                .build();
    }

    private TaskEventProcessResultVO buildDuplicateResult(TaskEventEntity event) {
        RewardGrantEntity grant = rewardGrantRepository.findByEventId(event.getEventId()).orElse(null);
        Integer walletCent = grant == null ? 0 : grant.getWalletCent();
        Integer growth = grant == null ? 0 : grant.getGrowth();
        Integer todayGranted = null;
        Integer remain = null;
        if (POST_CREATE.equals(event.getEventType())) {
            TaskConfigEntity config = taskConfigRepository.findByTaskType(POST_CREATE).orElse(null);
            int limit = config == null || config.getDailyLimit() == null ? 0 : config.getDailyLimit();
            int granted = dailyCounterRepository.findByUserIdAndTaskTypeAndBizDate(event.getActorUserId(), POST_CREATE, LocalDate.now())
                    .map(DailyCounterEntity::getGrantedCount)
                    .orElse(0);
            todayGranted = granted;
            remain = Math.max(0, limit - granted);
        }
        return TaskEventProcessResultVO.builder()
                .eventId(event.getEventId())
                .taskType(event.getEventType())
                .grantStatus(grant == null ? event.getProcessStatus() : grant.getStatus())
                .rewardWalletCent(walletCent)
                .rewardGrowth(growth)
                .todayGrantedCount(todayGranted)
                .todayRemainingCount(remain)
                .reason("EVENT_DUPLICATE")
                .duplicate(true)
                .build();
    }

    private TaskEventProcessResultVO markSkipped(TaskEventEntity event,
                                                 TaskConfigEntity config,
                                                 String status,
                                                 String reason,
                                                 int walletCent,
                                                 int growth,
                                                 Integer todayCount,
                                                 Integer remaining) {
        RewardGrantEntity grant = new RewardGrantEntity();
        grant.setGrantId("g_" + event.getEventId());
        grant.setEventId(event.getEventId());
        grant.setUserId(event.getActorUserId());
        grant.setTaskType(event.getEventType());
        grant.setWalletCent(walletCent);
        grant.setGrowth(growth);
        grant.setStatus(status);
        grant.setReason(reason);
        rewardGrantRepository.save(grant);

        event.setProcessStatus(status);
        event.setProcessReason(reason);
        taskEventRepository.save(event);

        return TaskEventProcessResultVO.builder()
                .eventId(event.getEventId())
                .taskType(event.getEventType())
                .grantStatus(status)
                .rewardWalletCent(walletCent)
                .rewardGrowth(growth)
                .todayGrantedCount(todayCount)
                .todayRemainingCount(remaining)
                .reason(reason)
                .duplicate(false)
                .build();
    }

    private DailyCounterEntity lockOrCreateCounter(Long userId, String taskType, LocalDate date) {
        Optional<DailyCounterEntity> existed = dailyCounterRepository.findByUserAndTaskAndDateForUpdate(userId, taskType, date);
        if (existed.isPresent()) {
            return existed.get();
        }

        DailyCounterEntity entity = new DailyCounterEntity();
        entity.setUserId(userId);
        entity.setTaskType(taskType);
        entity.setBizDate(date);
        entity.setGrantedCount(0);
        try {
            dailyCounterRepository.save(entity);
        } catch (DataIntegrityViolationException ignored) {
            // ignore duplicate insert in high concurrency
        }
        return dailyCounterRepository.findByUserAndTaskAndDateForUpdate(userId, taskType, date)
                .orElseGet(() -> {
                    DailyCounterEntity fallback = new DailyCounterEntity();
                    fallback.setUserId(userId);
                    fallback.setTaskType(taskType);
                    fallback.setBizDate(date);
                    fallback.setGrantedCount(0);
                    return dailyCounterRepository.save(fallback);
                });
    }

    private void validateRequest(TaskEventRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        if (isBlank(request.getEventId())) {
            throw new IllegalArgumentException("eventId 不能为空");
        }
        if (isBlank(request.getEventType())) {
            throw new IllegalArgumentException("eventType 不能为空");
        }
        if (isBlank(request.getBizId())) {
            throw new IllegalArgumentException("bizId 不能为空");
        }
        if (request.getActorUserId() == null || request.getActorUserId() <= 0) {
            throw new IllegalArgumentException("actorUserId 非法");
        }
        if (request.getCreatedAt() == null) {
            request.setCreatedAt(Instant.now());
        }
    }

    private TaskEventEntity buildTaskEvent(TaskEventRequestDTO request) {
        TaskEventEntity event = new TaskEventEntity();
        event.setEventId(request.getEventId());
        event.setEventType(request.getEventType());
        event.setBizId(request.getBizId());
        event.setActorUserId(request.getActorUserId());
        event.setTargetId(request.getTargetId());
        event.setEventCreatedAt(request.getCreatedAt());
        event.setBizDate(LocalDate.now());
        event.setMetaJson(toJson(request.getMeta()));
        event.setProcessStatus("RECEIVED");
        event.setProcessReason("INIT");
        return event;
    }

    private String validateEventState(TaskEventRequestDTO request) {
        Map<String, Object> meta = request.getMeta();
        if (POST_CREATE.equals(request.getEventType())) {
            if (readBoolean(meta, "isDraft") || readBoolean(meta, "isDeleted") || readBoolean(meta, "isBlocked")) {
                return "EVENT_INVALID_STATE";
            }
        }
        if (REPLY_CREATE.equals(request.getEventType())) {
            if (readBoolean(meta, "isDeleted") || readBoolean(meta, "isBlocked") || readBoolean(meta, "isSystemBackfill")) {
                return "EVENT_INVALID_STATE";
            }
        }
        return null;
    }

    private String evaluateRisk(TaskEventRequestDTO request, TaskConfigEntity config) {
        if (!REPLY_CREATE.equals(request.getEventType())) {
            return null;
        }
        if (isBlank(request.getTargetId())) {
            return "RISK_TARGET_REQUIRED";
        }

        int minIntervalSec = 10;
        int maxRepliesPerMinutePerPost = 5;
        try {
            if (!isBlank(config.getRiskPolicyJson())) {
                Map<String, Object> policy = objectMapper.readValue(config.getRiskPolicyJson(), Map.class);
                Object interval = policy.get("replySameTargetMinIntervalSec");
                Object perMinute = policy.get("replyMaxPerMinutePerPost");
                if (interval instanceof Number number) {
                    minIntervalSec = Math.max(1, number.intValue());
                }
                if (perMinute instanceof Number number) {
                    maxRepliesPerMinutePerPost = Math.max(1, number.intValue());
                }
            }
        } catch (Exception ignored) {
            // keep defaults
        }

        Optional<TaskEventEntity> latest = taskEventRepository.findLatestGrantedByUserAndTarget(
                request.getActorUserId(), REPLY_CREATE, request.getTargetId());
        if (latest.isPresent() && latest.get().getEventCreatedAt().plusSeconds(minIntervalSec).isAfter(request.getCreatedAt())) {
            return "RISK_REPLY_TOO_FREQUENT";
        }

        long recentCount = taskEventRepository.countGrantedSince(
                request.getActorUserId(), REPLY_CREATE, request.getTargetId(), request.getCreatedAt().minusSeconds(60));
        if (recentCount >= maxRepliesPerMinutePerPost) {
            return "RISK_REPLY_RATE_LIMIT";
        }
        return null;
    }

    private void initDefaultTaskConfigIfAbsent() {
        if (taskConfigRepository.findByTaskType(POST_CREATE).isEmpty()) {
            TaskConfigEntity post = new TaskConfigEntity();
            post.setTaskType(POST_CREATE);
            post.setEnabled(true);
            post.setRewardWalletCent(500);
            post.setRewardGrowth(0);
            post.setDailyLimit(3);
            taskConfigRepository.save(post);
        }
        if (taskConfigRepository.findByTaskType(REPLY_CREATE).isEmpty()) {
            TaskConfigEntity reply = new TaskConfigEntity();
            reply.setTaskType(REPLY_CREATE);
            reply.setEnabled(true);
            reply.setRewardWalletCent(0);
            reply.setRewardGrowth(15);
            reply.setDailyLimit(null);
            reply.setRiskPolicyJson("{\"replySameTargetMinIntervalSec\":10,\"replyMaxPerMinutePerPost\":5}");
            taskConfigRepository.save(reply);
        }
    }

    private WalletAccountEntity createWalletForUser(UserEntity user) {
        WalletAccountEntity wallet = new WalletAccountEntity();
        wallet.setWalletNo("W" + user.getAccount());
        wallet.setUserId(user.getId());
        wallet.setBalance(BigDecimal.ZERO.setScale(2));
        wallet.setBalanceCent(0L);
        wallet.setRewardVersion(0);
        wallet.setCurrency("CNY");
        wallet.setStatus("ACTIVE");
        return walletAccountRepository.save(wallet);
    }

    private long decimalToCent(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }
        return amount.movePointRight(2).longValue();
    }

    private String toJson(Map<String, Object> meta) {
        if (meta == null || meta.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(meta);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private boolean readBoolean(Map<String, Object> meta, String key) {
        if (meta == null || !meta.containsKey(key)) {
            return false;
        }
        Object value = meta.get(key);
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            return "true".equalsIgnoreCase(s);
        }
        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
