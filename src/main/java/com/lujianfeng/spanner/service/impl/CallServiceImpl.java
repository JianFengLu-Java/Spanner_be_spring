package com.lujianfeng.spanner.service.impl;

import com.lujianfeng.spanner.dto.call.CallActionRequestDTO;
import com.lujianfeng.spanner.dto.call.CallCreateRequestDTO;
import com.lujianfeng.spanner.dto.call.CallSignalRequestDTO;
import com.lujianfeng.spanner.entity.call.CallRequestLogEntity;
import com.lujianfeng.spanner.entity.call.CallSessionEntity;
import com.lujianfeng.spanner.entity.call.CallSignalTypeEnum;
import com.lujianfeng.spanner.entity.call.CallStatusEnum;
import com.lujianfeng.spanner.entity.call.CallTypeEnum;
import com.lujianfeng.spanner.entity.user.UserEntity;
import com.lujianfeng.spanner.entity.user.UserRelationEnum;
import com.lujianfeng.spanner.repository.CallRequestLogRepository;
import com.lujianfeng.spanner.repository.CallSessionRepository;
import com.lujianfeng.spanner.repository.UserRelationRepository;
import com.lujianfeng.spanner.repository.UserRepository;
import com.lujianfeng.spanner.service.CallBizException;
import com.lujianfeng.spanner.service.CallService;
import com.lujianfeng.spanner.service.service.UserService;
import com.lujianfeng.spanner.vo.call.CallActionResultVO;
import com.lujianfeng.spanner.vo.call.CallSessionVO;
import com.lujianfeng.spanner.vo.call.RtcIceServersVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class CallServiceImpl implements CallService {

    private static final Set<CallStatusEnum> ACTIVE_STATUSES = Set.of(
            CallStatusEnum.RINGING,
            CallStatusEnum.ANSWERED,
            CallStatusEnum.CONNECTING,
            CallStatusEnum.CONNECTED
    );

    private final CallSessionRepository callSessionRepository;
    private final CallRequestLogRepository callRequestLogRepository;
    private final UserRepository userRepository;
    private final UserRelationRepository userRelationRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    public CallServiceImpl(CallSessionRepository callSessionRepository,
                           CallRequestLogRepository callRequestLogRepository,
                           UserRepository userRepository,
                           UserRelationRepository userRelationRepository,
                           UserService userService,
                           SimpMessagingTemplate messagingTemplate) {
        this.callSessionRepository = callSessionRepository;
        this.callRequestLogRepository = callRequestLogRepository;
        this.userRepository = userRepository;
        this.userRelationRepository = userRelationRepository;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    @Transactional
    public CallActionResultVO createCall(CallCreateRequestDTO requestDTO) {
        String requestId = requireRequestId(requestDTO == null ? null : requestDTO.getRequestId());
        String actor = currentAccount();

        Optional<CallActionResultVO> replay = resolveIdempotent(requestId, "CREATE", actor, null);
        if (replay.isPresent()) {
            return replay.get();
        }

        String calleeAccount = trim(requestDTO == null ? null : requestDTO.getCalleeAccount());
        if (calleeAccount == null) {
            throw new CallBizException(400, "CALL_INVALID_PARAM", "calleeAccount 不能为空");
        }
        if (actor.equals(calleeAccount)) {
            throw new CallBizException(400, "CALL_INVALID_PARAM", "不能呼叫自己");
        }

        UserEntity caller = requireUser(actor);
        UserEntity callee = userRepository.findByAccount(calleeAccount);
        if (callee == null) {
            throw new CallBizException(404, "CALL_NOT_FOUND", "被叫用户不存在");
        }
        if (!isFriend(caller, callee)) {
            throw new CallBizException(403, "CALL_FORBIDDEN", "仅支持好友之间发起通话");
        }

        cleanupStaleActiveSessions(actor);
        cleanupStaleActiveSessions(calleeAccount);

        if (callSessionRepository.countActiveByAccount(actor, ACTIVE_STATUSES) > 0) {
            throw new CallBizException(409, "CALL_STATE_CONFLICT", "当前账号已有进行中的通话");
        }
        if (callSessionRepository.countActiveByAccount(calleeAccount, ACTIVE_STATUSES) > 0) {
            throw new CallBizException(409, "CALLEE_BUSY", "被叫正忙，请稍后重试");
        }

        LocalDateTime now = LocalDateTime.now();
        CallTypeEnum callType = parseCallType(requestDTO == null ? null : requestDTO.getType());
        CallSessionEntity entity = new CallSessionEntity();
        entity.setCallId(nextCallId());
        entity.setType(callType);
        entity.setStatus(CallStatusEnum.RINGING);
        entity.setCallerAccount(actor);
        entity.setCallerName(displayName(caller));
        entity.setCallerAvatar(caller.getAvatarUrl());
        entity.setCalleeAccount(calleeAccount);
        entity.setCalleeName(displayName(callee));
        entity.setCalleeAvatar(callee.getAvatarUrl());
        entity.setChannelId(buildPrivateChannel(actor, calleeAccount));
        entity.setStartedAt(now);
        entity.setExpiresAt(now.plusSeconds(45));

        CallSessionEntity saved = callSessionRepository.save(entity);
        saveRequestLog(requestId, saved.getCallId(), actor, "CREATE", saved.getStatus().name());

        pushIncomingCall(saved);
        pushCallEvent(saved.getCallerAccount(), "call.ringing", Map.of(
                "callId", saved.getCallId(),
                "status", saved.getStatus().name(),
                "expiresAt", toUtc(saved.getExpiresAt())
        ));

        return toActionVO(saved);
    }

    @Override
    @Transactional
    public CallActionResultVO acceptCall(String callId, CallActionRequestDTO requestDTO) {
        String requestId = requireRequestId(requestDTO == null ? null : requestDTO.getRequestId());
        String actor = currentAccount();

        Optional<CallActionResultVO> replay = resolveIdempotent(requestId, "ACCEPT", actor, callId);
        if (replay.isPresent()) {
            return replay.get();
        }

        CallSessionEntity session = loadForUpdate(callId);
        ensureParticipant(session, actor);
        if (!actor.equals(session.getCalleeAccount())) {
            throw new CallBizException(403, "CALL_FORBIDDEN", "仅被叫可执行接听");
        }

        if (session.getStatus() == CallStatusEnum.ANSWERED
                || session.getStatus() == CallStatusEnum.CONNECTING
                || session.getStatus() == CallStatusEnum.CONNECTED) {
            saveRequestLog(requestId, session.getCallId(), actor, "ACCEPT", session.getStatus().name());
            return toActionVO(session);
        }

        if (session.getStatus() != CallStatusEnum.RINGING) {
            throw new CallBizException(409, "CALL_STATE_CONFLICT", "当前状态不允许接听");
        }
        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new CallBizException(410, "CALL_EXPIRED", "通话已超时");
        }

        session.setStatus(CallStatusEnum.ANSWERED);
        session.setAnsweredAt(LocalDateTime.now());
        callSessionRepository.save(session);
        saveRequestLog(requestId, session.getCallId(), actor, "ACCEPT", session.getStatus().name());

        Map<String, Object> payload = Map.of(
                "callId", session.getCallId(),
                "answeredBy", actor,
                "answeredAt", toUtc(session.getAnsweredAt())
        );
        pushCallEvent(session.getCallerAccount(), "call.answered", payload);
        pushCallEvent(session.getCalleeAccount(), "call.answered", payload);

        return toActionVO(session);
    }

    @Override
    @Transactional
    public CallActionResultVO rejectCall(String callId, CallActionRequestDTO requestDTO) {
        String requestId = requireRequestId(requestDTO == null ? null : requestDTO.getRequestId());
        String actor = currentAccount();

        Optional<CallActionResultVO> replay = resolveIdempotent(requestId, "REJECT", actor, callId);
        if (replay.isPresent()) {
            return replay.get();
        }

        CallSessionEntity session = loadForUpdate(callId);
        ensureParticipant(session, actor);
        if (!actor.equals(session.getCalleeAccount())) {
            throw new CallBizException(403, "CALL_FORBIDDEN", "仅被叫可执行拒绝");
        }

        if (session.getStatus() == CallStatusEnum.REJECTED) {
            saveRequestLog(requestId, session.getCallId(), actor, "REJECT", session.getStatus().name());
            return toActionVO(session);
        }
        if (session.getStatus() != CallStatusEnum.RINGING) {
            throw new CallBizException(409, "CALL_STATE_CONFLICT", "当前状态不允许拒绝");
        }

        LocalDateTime now = LocalDateTime.now();
        session.setStatus(CallStatusEnum.REJECTED);
        session.setEndedAt(now);
        session.setEndReason(trim(requestDTO == null ? null : requestDTO.getReason()) == null
                ? "REJECTED_BY_USER"
                : trim(requestDTO == null ? null : requestDTO.getReason()));
        callSessionRepository.save(session);
        saveRequestLog(requestId, session.getCallId(), actor, "REJECT", session.getStatus().name());

        pushEndedToBoth(session, actor);
        return toActionVO(session);
    }

    @Override
    @Transactional
    public CallActionResultVO cancelCall(String callId, CallActionRequestDTO requestDTO) {
        String requestId = requireRequestId(requestDTO == null ? null : requestDTO.getRequestId());
        String actor = currentAccount();

        Optional<CallActionResultVO> replay = resolveIdempotent(requestId, "CANCEL", actor, callId);
        if (replay.isPresent()) {
            return replay.get();
        }

        CallSessionEntity session = loadForUpdate(callId);
        ensureParticipant(session, actor);
        if (!actor.equals(session.getCallerAccount())) {
            throw new CallBizException(403, "CALL_FORBIDDEN", "仅主叫可执行取消");
        }

        if (session.getStatus() == CallStatusEnum.CANCELED) {
            saveRequestLog(requestId, session.getCallId(), actor, "CANCEL", session.getStatus().name());
            return toActionVO(session);
        }
        if (session.getStatus() != CallStatusEnum.RINGING) {
            throw new CallBizException(409, "CALL_STATE_CONFLICT", "当前状态不允许取消");
        }

        session.setStatus(CallStatusEnum.CANCELED);
        session.setEndedAt(LocalDateTime.now());
        session.setEndReason("CANCELED_BY_CALLER");
        callSessionRepository.save(session);
        saveRequestLog(requestId, session.getCallId(), actor, "CANCEL", session.getStatus().name());

        pushEndedToBoth(session, actor);
        return toActionVO(session);
    }

    @Override
    @Transactional
    public CallActionResultVO endCall(String callId, CallActionRequestDTO requestDTO) {
        String requestId = requireRequestId(requestDTO == null ? null : requestDTO.getRequestId());
        String actor = currentAccount();

        Optional<CallActionResultVO> replay = resolveIdempotent(requestId, "END", actor, callId);
        if (replay.isPresent()) {
            return replay.get();
        }

        CallSessionEntity session = loadForUpdate(callId);
        ensureParticipant(session, actor);

        if (session.getStatus().isTerminal()) {
            saveRequestLog(requestId, session.getCallId(), actor, "END", session.getStatus().name());
            return toActionVO(session);
        }
        if (session.getStatus() != CallStatusEnum.ANSWERED
                && session.getStatus() != CallStatusEnum.CONNECTING
                && session.getStatus() != CallStatusEnum.CONNECTED) {
            throw new CallBizException(409, "CALL_STATE_CONFLICT", "当前状态不允许挂断");
        }

        LocalDateTime now = LocalDateTime.now();
        session.setStatus(CallStatusEnum.ENDED);
        session.setEndedAt(now);
        session.setEndReason(trim(requestDTO == null ? null : requestDTO.getReason()) == null
                ? "HANGUP"
                : trim(requestDTO == null ? null : requestDTO.getReason()));
        session.setDurationSeconds(calcDuration(session));
        callSessionRepository.save(session);
        saveRequestLog(requestId, session.getCallId(), actor, "END", session.getStatus().name());

        pushEndedToBoth(session, actor);
        return toActionVO(session);
    }

    @Override
    @Transactional(readOnly = true)
    public CallSessionVO getCall(String callId) {
        String actor = currentAccount();
        CallSessionEntity session = callSessionRepository.findByCallId(callId)
                .orElseThrow(() -> new CallBizException(404, "CALL_NOT_FOUND", "通话不存在"));
        ensureParticipant(session, actor);
        return toSessionVO(session);
    }

    @Override
    @Transactional
    public Map<String, Object> sendSignal(String callId, CallSignalRequestDTO requestDTO) {
        String requestId = requireRequestId(requestDTO == null ? null : requestDTO.getRequestId());
        String actor = currentAccount();
        boolean duplicatedRequestId = isDuplicatedSignalRequest(requestId, actor, callId);

        CallSessionEntity session = loadForUpdate(callId);
        ensureParticipant(session, actor);
        if (session.getStatus().isTerminal()) {
            throw new CallBizException(409, "CALL_STATE_CONFLICT", "通话已结束，不能发送信令");
        }

        String target = trim(requestDTO == null ? null : requestDTO.getTo());
        if (target == null) {
            throw new CallBizException(400, "CALL_INVALID_PARAM", "to 不能为空");
        }
        String peer = actor.equals(session.getCallerAccount()) ? session.getCalleeAccount() : session.getCallerAccount();
        if (!target.equals(peer)) {
            throw new CallBizException(403, "CALL_FORBIDDEN", "to 不是通话对端");
        }

        CallSignalTypeEnum signalType = parseSignalType(requestDTO == null ? null : requestDTO.getSignalType());
        validateSignal(signalType, requestDTO);
        log.info("Receive call signal, callId={}, from={}, to={}, signalType={}, requestId={}, status={}",
                session.getCallId(), actor, target, signalType, requestId, session.getStatus());

        if ((signalType == CallSignalTypeEnum.OFFER || signalType == CallSignalTypeEnum.ANSWER)
                && session.getStatus() == CallStatusEnum.ANSWERED) {
            session.setStatus(CallStatusEnum.CONNECTING);
            callSessionRepository.save(session);
        }
        if (signalType == CallSignalTypeEnum.ICE_CANDIDATE
                && session.getStatus() == CallStatusEnum.CONNECTING
                && session.getConnectedAt() == null) {
            session.setStatus(CallStatusEnum.CONNECTED);
            session.setConnectedAt(LocalDateTime.now());
            callSessionRepository.save(session);
        }

        Map<String, Object> signalPayload = new HashMap<>();
        signalPayload.put("callId", session.getCallId());
        signalPayload.put("signalType", signalType.name());
        signalPayload.put("from", actor);
        signalPayload.put("to", target);
        signalPayload.put("sdp", trim(requestDTO == null ? null : requestDTO.getSdp()));
        signalPayload.put("candidate", trim(requestDTO == null ? null : requestDTO.getCandidate()));
        signalPayload.put("sdpMid", trim(requestDTO == null ? null : requestDTO.getSdpMid()));
        signalPayload.put("sdpMLineIndex", requestDTO == null ? null : requestDTO.getSdpMLineIndex());
        signalPayload.put("createdAt", toUtc(LocalDateTime.now()));

        pushSignalEvent(target, signalPayload);
        if (!duplicatedRequestId) {
            saveRequestLog(requestId, session.getCallId(), actor, "SIGNAL", session.getStatus().name());
        } else {
            log.warn("Duplicated SIGNAL requestId detected, requestId={}, callId={}, actor={}. Forwarded again for reliability.",
                    requestId, session.getCallId(), actor);
        }
        log.info("Dispatch call signal success, callId={}, from={}, to={}, signalType={}, requestId={}",
                session.getCallId(), actor, target, signalType, requestId);

        return Map.of("accepted", true);
    }

    @Override
    @Transactional
    public Map<String, Object> heartbeat(String callId, CallActionRequestDTO requestDTO) {
        String requestId = requireRequestId(requestDTO == null ? null : requestDTO.getRequestId());
        String actor = currentAccount();

        Optional<CallActionResultVO> replay = resolveIdempotent(requestId, "HEARTBEAT", actor, callId);
        if (replay.isPresent()) {
            return Map.of("alive", true, "status", replay.get().getStatus());
        }

        CallSessionEntity session = loadForUpdate(callId);
        ensureParticipant(session, actor);
        session.setLastHeartbeatAt(LocalDateTime.now());
        callSessionRepository.save(session);
        saveRequestLog(requestId, session.getCallId(), actor, "HEARTBEAT", session.getStatus().name());

        return Map.of("alive", true, "status", session.getStatus().name());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> history(Integer page, Integer size) {
        String actor = currentAccount();
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 20 : Math.min(size, 100);

        Pageable pageable = PageRequest.of(safePage - 1, safeSize);
        Page<CallSessionEntity> result = callSessionRepository
                .findByCallerAccountOrCalleeAccountOrderByStartedAtDesc(actor, actor, pageable);

        List<CallSessionVO> items = result.getContent().stream().map(this::toSessionVO).toList();
        Map<String, Object> data = new HashMap<>();
        data.put("items", items);
        data.put("page", safePage);
        data.put("size", safeSize);
        data.put("total", result.getTotalElements());
        data.put("totalPages", result.getTotalPages());
        data.put("hasMore", result.hasNext());
        return data;
    }

    @Override
    public RtcIceServersVO listIceServers() {
        RtcIceServersVO.IceServerItemVO stun = RtcIceServersVO.IceServerItemVO.builder()
                .urls(List.of("stun:stun.l.google.com:19302"))
                .build();
        return RtcIceServersVO.builder()
                .servers(List.of(stun))
                .build();
    }

    @Override
    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void markNoAnswerTimeoutSessions() {
        LocalDateTime now = LocalDateTime.now();
        List<CallSessionEntity> expired = callSessionRepository.findExpiredByStatus(CallStatusEnum.RINGING, now);
        for (CallSessionEntity item : expired) {
            try {
                Optional<CallSessionEntity> maybeLocked = callSessionRepository.findByCallIdForUpdate(item.getCallId());
                if (maybeLocked.isEmpty()) {
                    continue;
                }
                CallSessionEntity session = maybeLocked.get();
                if (session.getStatus() != CallStatusEnum.RINGING) {
                    continue;
                }
                if (session.getExpiresAt() == null || !session.getExpiresAt().isBefore(LocalDateTime.now())) {
                    continue;
                }
                session.setStatus(CallStatusEnum.NO_ANSWER);
                session.setEndedAt(LocalDateTime.now());
                session.setEndReason("NO_ANSWER_TIMEOUT");
                callSessionRepository.save(session);
                pushEndedToBoth(session, "SYSTEM");
            } catch (Exception e) {
                log.warn("Mark no-answer timeout failed, callId={}", item.getCallId(), e);
            }
        }
    }

    private CallSessionEntity loadForUpdate(String callId) {
        String normalized = trim(callId);
        if (normalized == null) {
            throw new CallBizException(400, "CALL_INVALID_PARAM", "callId 不能为空");
        }
        return callSessionRepository.findByCallIdForUpdate(normalized)
                .orElseThrow(() -> new CallBizException(404, "CALL_NOT_FOUND", "通话不存在"));
    }

    private String currentAccount() {
        UserEntity current = userService.getCurrentUserEntity();
        if (current == null || trim(current.getAccount()) == null) {
            throw new CallBizException(401, "UNAUTHORIZED", "未登录");
        }
        return current.getAccount().trim();
    }

    private UserEntity requireUser(String account) {
        UserEntity user = userRepository.findByAccount(account);
        if (user == null) {
            throw new CallBizException(404, "CALL_NOT_FOUND", "用户不存在");
        }
        return user;
    }

    private boolean isFriend(UserEntity from, UserEntity to) {
        return userRelationRepository.existsByUserAndFriendAndRelationType(from, to, UserRelationEnum.ACCEPTED)
                || userRelationRepository.existsByUserAndFriendAndRelationType(to, from, UserRelationEnum.ACCEPTED);
    }

    private String requireRequestId(String requestId) {
        String normalized = trim(requestId);
        if (normalized == null) {
            throw new CallBizException(400, "CALL_INVALID_PARAM", "requestId 不能为空");
        }
        return normalized;
    }

    private Optional<CallActionResultVO> resolveIdempotent(String requestId,
                                                            String action,
                                                            String actor,
                                                            String callId) {
        Optional<CallRequestLogEntity> existing = callRequestLogRepository.findByRequestId(requestId);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        CallRequestLogEntity logEntity = existing.get();
        if (!action.equals(logEntity.getAction()) || !actor.equals(logEntity.getActorAccount())) {
            throw new CallBizException(409, "CALL_STATE_CONFLICT", "requestId 已被其他操作占用");
        }
        if (callId != null && logEntity.getCallId() != null && !callId.equals(logEntity.getCallId())) {
            throw new CallBizException(409, "CALL_STATE_CONFLICT", "requestId 与 callId 不匹配");
        }
        if (logEntity.getCallId() == null) {
            return Optional.empty();
        }
        CallSessionEntity session = callSessionRepository.findByCallId(logEntity.getCallId())
                .orElseThrow(() -> new CallBizException(404, "CALL_NOT_FOUND", "通话不存在"));
        ensureParticipant(session, actor);
        return Optional.of(toActionVO(session));
    }

    private boolean isDuplicatedSignalRequest(String requestId,
                                              String actor,
                                              String callId) {
        Optional<CallRequestLogEntity> existing = callRequestLogRepository.findByRequestId(requestId);
        if (existing.isEmpty()) {
            return false;
        }
        CallRequestLogEntity logEntity = existing.get();
        if (!"SIGNAL".equals(logEntity.getAction()) || !actor.equals(logEntity.getActorAccount())) {
            throw new CallBizException(409, "CALL_STATE_CONFLICT", "requestId 已被其他操作占用");
        }
        if (logEntity.getCallId() != null && !callId.equals(logEntity.getCallId())) {
            throw new CallBizException(409, "CALL_STATE_CONFLICT", "requestId 与 callId 不匹配");
        }
        return true;
    }

    private void saveRequestLog(String requestId,
                                String callId,
                                String actor,
                                String action,
                                String resultStatus) {
        CallRequestLogEntity logEntity = new CallRequestLogEntity();
        logEntity.setRequestId(requestId);
        logEntity.setCallId(callId);
        logEntity.setActorAccount(actor);
        logEntity.setAction(action);
        logEntity.setResultStatus(resultStatus);
        callRequestLogRepository.save(logEntity);
    }

    private CallTypeEnum parseCallType(String raw) {
        String normalized = trim(raw);
        if (normalized == null) {
            return CallTypeEnum.VIDEO;
        }
        try {
            return CallTypeEnum.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new CallBizException(400, "CALL_INVALID_PARAM", "type 仅支持 VIDEO/AUDIO");
        }
    }

    private CallSignalTypeEnum parseSignalType(String raw) {
        String normalized = trim(raw);
        if (normalized == null) {
            throw new CallBizException(400, "CALL_INVALID_PARAM", "signalType 不能为空");
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if ("RENEGOTIATE".equals(upper) || "RENEGOTIATION".equals(upper)) {
            upper = "RENEGOTIATE";
        }
        try {
            return CallSignalTypeEnum.valueOf(upper);
        } catch (Exception e) {
            throw new CallBizException(422, "RTC_SIGNAL_INVALID", "signalType 非法");
        }
    }

    private void validateSignal(CallSignalTypeEnum signalType, CallSignalRequestDTO requestDTO) {
        if ((signalType == CallSignalTypeEnum.OFFER || signalType == CallSignalTypeEnum.ANSWER)
                && trim(requestDTO == null ? null : requestDTO.getSdp()) == null) {
            throw new CallBizException(422, "RTC_SIGNAL_INVALID", "OFFER/ANSWER 需要 sdp");
        }
        if (signalType == CallSignalTypeEnum.ICE_CANDIDATE
                && trim(requestDTO == null ? null : requestDTO.getCandidate()) == null) {
            throw new CallBizException(422, "RTC_SIGNAL_INVALID", "ICE_CANDIDATE 需要 candidate");
        }
    }

    private void ensureParticipant(CallSessionEntity session, String actor) {
        if (!actor.equals(session.getCallerAccount()) && !actor.equals(session.getCalleeAccount())) {
            throw new CallBizException(403, "CALL_FORBIDDEN", "无权操作该通话");
        }
    }

    private void pushIncomingCall(CallSessionEntity session) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("callId", session.getCallId());
        payload.put("from", session.getCallerAccount());
        payload.put("fromName", session.getCallerName());
        payload.put("fromAvatar", session.getCallerAvatar());
        payload.put("type", session.getType().name().toLowerCase(Locale.ROOT));
        payload.put("chatId", parseChatId(session.getCallerAccount()));
        payload.put("createdAt", toUtc(session.getStartedAt()));
        pushCallEvent(session.getCalleeAccount(), "incoming.call", payload);
    }

    private void pushEndedToBoth(CallSessionEntity session, String endedBy) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("callId", session.getCallId());
        payload.put("status", session.getStatus().name());
        payload.put("endReason", session.getEndReason());
        payload.put("endedAt", toUtc(session.getEndedAt()));
        payload.put("durationSeconds", session.getDurationSeconds());
        payload.put("callerAccount", session.getCallerAccount());
        payload.put("calleeAccount", session.getCalleeAccount());
        payload.put("endedBy", endedBy);

        pushCallEvent(session.getCallerAccount(), "call.ended", payload);
        pushCallEvent(session.getCalleeAccount(), "call.ended", payload);
    }

    private void pushCallEvent(String account, String event, Map<String, Object> payload) {
        messagingTemplate.convertAndSendToUser(account, "/queue/calls", Map.of(
                "event", event,
                "payload", payload
        ));
    }

    private void pushSignalEvent(String account, Map<String, Object> payload) {
        messagingTemplate.convertAndSendToUser(account, "/queue/call-signals", Map.of(
                "event", "call.signal",
                "payload", payload
        ));
    }

    private void cleanupStaleActiveSessions(String account) {
        List<CallSessionEntity> active = callSessionRepository.findActiveByAccount(account, ACTIVE_STATUSES);
        LocalDateTime now = LocalDateTime.now();
        for (CallSessionEntity item : active) {
            Optional<CallSessionEntity> maybeLocked = callSessionRepository.findByCallIdForUpdate(item.getCallId());
            if (maybeLocked.isEmpty()) {
                continue;
            }
            CallSessionEntity session = maybeLocked.get();
            if (!ACTIVE_STATUSES.contains(session.getStatus())) {
                continue;
            }
            if (session.getStatus() == CallStatusEnum.RINGING
                    && session.getExpiresAt() != null
                    && session.getExpiresAt().isBefore(now)) {
                session.setStatus(CallStatusEnum.NO_ANSWER);
                session.setEndedAt(now);
                session.setEndReason("NO_ANSWER_TIMEOUT");
                callSessionRepository.save(session);
                pushEndedToBoth(session, "SYSTEM");
                continue;
            }
            if ((session.getStatus() == CallStatusEnum.ANSWERED || session.getStatus() == CallStatusEnum.CONNECTING)
                    && isBeforeNow(session.getUpdatedAt(), now, Duration.ofMinutes(2))) {
                session.setStatus(CallStatusEnum.FAILED);
                session.setEndedAt(now);
                session.setEndReason("SIGNAL_NEGOTIATION_TIMEOUT");
                callSessionRepository.save(session);
                pushEndedToBoth(session, "SYSTEM");
                continue;
            }
            if (session.getStatus() == CallStatusEnum.CONNECTED
                    && isBeforeNow(session.getConnectedAt(), now, Duration.ofHours(8))) {
                session.setStatus(CallStatusEnum.ENDED);
                session.setEndedAt(now);
                session.setEndReason("SESSION_STALE_TIMEOUT");
                session.setDurationSeconds(calcDuration(session));
                callSessionRepository.save(session);
                pushEndedToBoth(session, "SYSTEM");
            }
        }
    }

    private boolean isBeforeNow(LocalDateTime time, LocalDateTime now, Duration timeout) {
        if (time == null || timeout == null) {
            return false;
        }
        return time.plus(timeout).isBefore(now);
    }

    private CallActionResultVO toActionVO(CallSessionEntity session) {
        return CallActionResultVO.builder()
                .callId(session.getCallId())
                .status(session.getStatus().name())
                .answeredAt(toUtc(session.getAnsweredAt()))
                .endedAt(toUtc(session.getEndedAt()))
                .endReason(session.getEndReason())
                .durationSeconds(session.getDurationSeconds())
                .expiresAt(toUtc(session.getExpiresAt()))
                .build();
    }

    private CallSessionVO toSessionVO(CallSessionEntity session) {
        return CallSessionVO.builder()
                .callId(session.getCallId())
                .type(session.getType() == null ? null : session.getType().name())
                .status(session.getStatus() == null ? null : session.getStatus().name())
                .callerAccount(session.getCallerAccount())
                .callerName(session.getCallerName())
                .callerAvatar(session.getCallerAvatar())
                .calleeAccount(session.getCalleeAccount())
                .calleeName(session.getCalleeName())
                .calleeAvatar(session.getCalleeAvatar())
                .channelId(session.getChannelId())
                .startedAt(toUtc(session.getStartedAt()))
                .expiresAt(toUtc(session.getExpiresAt()))
                .answeredAt(toUtc(session.getAnsweredAt()))
                .connectedAt(toUtc(session.getConnectedAt()))
                .endedAt(toUtc(session.getEndedAt()))
                .endReason(session.getEndReason())
                .durationSeconds(session.getDurationSeconds())
                .build();
    }

    private Long parseChatId(String account) {
        try {
            return account == null ? null : Long.parseLong(account);
        } catch (Exception e) {
            return null;
        }
    }

    private String buildPrivateChannel(String a, String b) {
        if (a.compareTo(b) <= 0) {
            return "private:" + a + ":" + b;
        }
        return "private:" + b + ":" + a;
    }

    private String displayName(UserEntity user) {
        if (user == null) {
            return null;
        }
        String realName = trim(user.getRealName());
        return realName == null ? user.getAccount() : realName;
    }

    private Long calcDuration(CallSessionEntity session) {
        if (session.getConnectedAt() == null || session.getEndedAt() == null) {
            return 0L;
        }
        long seconds = java.time.Duration.between(session.getConnectedAt(), session.getEndedAt()).getSeconds();
        return Math.max(seconds, 0L);
    }

    private String nextCallId() {
        return "call_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String toUtc(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC)
                .toInstant()
                .toString();
    }
}
