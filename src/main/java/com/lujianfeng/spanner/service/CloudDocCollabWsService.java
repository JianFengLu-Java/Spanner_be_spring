package com.lujianfeng.spanner.service;

import com.lujianfeng.spanner.dto.cloud.CloudDocWsCursorDTO;
import com.lujianfeng.spanner.dto.cloud.CloudDocWsPatchDTO;
import com.lujianfeng.spanner.entity.cloud.CloudDocEntity;
import com.lujianfeng.spanner.entity.cloud.CloudDocShareEntity;
import com.lujianfeng.spanner.repository.CloudDocRepository;
import com.lujianfeng.spanner.repository.CloudDocShareRepository;
import com.lujianfeng.spanner.vo.cloud.CloudDocWsAckVO;
import com.lujianfeng.spanner.vo.cloud.CloudDocWsCursorVO;
import com.lujianfeng.spanner.vo.cloud.CloudDocWsEventVO;
import com.lujianfeng.spanner.vo.cloud.CloudDocWsMemberVO;
import com.lujianfeng.spanner.vo.cloud.CloudDocWsPatchDataVO;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CloudDocCollabWsService {
    private static final String SHARE_MODE_COLLAB = "COLLAB";

    private final SimpMessagingTemplate messagingTemplate;
    private final CloudDocRepository cloudDocRepository;
    private final CloudDocShareRepository cloudDocShareRepository;

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, SessionState>> roomSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> sessionDocs = new ConcurrentHashMap<>();

    public CloudDocCollabWsService(SimpMessagingTemplate messagingTemplate,
                                   CloudDocRepository cloudDocRepository,
                                   CloudDocShareRepository cloudDocShareRepository) {
        this.messagingTemplate = messagingTemplate;
        this.cloudDocRepository = cloudDocRepository;
        this.cloudDocShareRepository = cloudDocShareRepository;
    }

    public CloudDocWsAckVO join(String account, String sessionId, String docIdRaw) {
        String docId = normalizeDocId(docIdRaw);
        ensureAccess(account, docId);

        ConcurrentHashMap<String, SessionState> docRoom = roomSessions.computeIfAbsent(docId, k -> new ConcurrentHashMap<>());
        docRoom.put(sessionId, new SessionState(account));
        sessionDocs.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).add(docId);

        broadcastPresenceSnapshot(docId);
        broadcastEvent(docId, account, "presence.join", Map.of("account", account));
        return ack("join", docId);
    }

    public CloudDocWsAckVO leave(String account, String sessionId, String docIdRaw) {
        String docId = normalizeDocId(docIdRaw);
        removeSessionFromDoc(sessionId, docId);
        broadcastPresenceSnapshot(docId);
        broadcastEvent(docId, account, "presence.leave", Map.of("account", account));
        return ack("leave", docId);
    }

    public CloudDocWsAckVO cursor(String account, String sessionId, CloudDocWsCursorDTO payload) {
        if (payload == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        String docId = normalizeDocId(payload.getDocId());
        ensureAccess(account, docId);

        ConcurrentHashMap<String, SessionState> docRoom = roomSessions.computeIfAbsent(docId, k -> new ConcurrentHashMap<>());
        SessionState sessionState = docRoom.computeIfAbsent(sessionId, k -> new SessionState(account));
        sessionState.cursorAnchor = normalizeCursor(payload.getAnchor());
        sessionState.cursorHead = normalizeCursor(payload.getHead());
        sessionDocs.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).add(docId);

        CloudDocWsCursorVO cursor = CloudDocWsCursorVO.builder()
                .anchor(sessionState.cursorAnchor)
                .head(sessionState.cursorHead)
                .build();
        broadcastEvent(docId, account, "cursor.update", cursor);
        return ack("cursor", docId);
    }

    public CloudDocWsAckVO patch(String account, String sessionId, CloudDocWsPatchDTO payload) {
        if (payload == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        String docId = normalizeDocId(payload.getDocId());
        ensureAccess(account, docId);

        roomSessions.computeIfAbsent(docId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(sessionId, k -> new SessionState(account));
        sessionDocs.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).add(docId);

        CloudDocWsPatchDataVO patchData = CloudDocWsPatchDataVO.builder()
                .baseVersion(payload.getBaseVersion())
                .opId(trim(payload.getOpId()))
                .opType(trim(payload.getOpType()))
                .payload(trim(payload.getPayload()))
                .build();
        broadcastEvent(docId, account, "content.patch", patchData);
        return ack("patch", docId);
    }

    public void handleDisconnect(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        Set<String> docs = sessionDocs.remove(sessionId);
        if (docs == null || docs.isEmpty()) {
            return;
        }
        for (String docId : docs) {
            removeSessionFromDoc(sessionId, docId);
            broadcastPresenceSnapshot(docId);
        }
    }

    private void broadcastPresenceSnapshot(String docId) {
        ConcurrentHashMap<String, SessionState> docRoom = roomSessions.get(docId);
        List<CloudDocWsMemberVO> members = buildMembers(docRoom);
        Map<String, Object> data = new HashMap<>();
        data.put("members", members);
        data.put("onlineCount", members.size());
        sendEventToDocMembers(docId, CloudDocWsEventVO.builder()
                .eventType("presence.snapshot")
                .docId(docId)
                .from("SYSTEM")
                .at(formatUtc(LocalDateTime.now()))
                .data(data)
                .build());
    }

    private List<CloudDocWsMemberVO> buildMembers(ConcurrentHashMap<String, SessionState> docRoom) {
        if (docRoom == null || docRoom.isEmpty()) {
            return List.of();
        }
        Map<String, SessionState> byAccount = new HashMap<>();
        for (SessionState state : docRoom.values()) {
            byAccount.putIfAbsent(state.account, state);
        }
        List<CloudDocWsMemberVO> members = new ArrayList<>();
        for (Map.Entry<String, SessionState> entry : byAccount.entrySet()) {
            SessionState state = entry.getValue();
            members.add(CloudDocWsMemberVO.builder()
                    .account(entry.getKey())
                    .cursor(CloudDocWsCursorVO.builder().anchor(state.cursorAnchor).head(state.cursorHead).build())
                    .build());
        }
        return members;
    }

    private void broadcastEvent(String docId, String from, String eventType, Object data) {
        CloudDocWsEventVO event = CloudDocWsEventVO.builder()
                .eventType(eventType)
                .docId(docId)
                .from(from)
                .at(formatUtc(LocalDateTime.now()))
                .data(data)
                .build();
        sendEventToDocMembers(docId, event);
    }

    private void sendEventToDocMembers(String docId, CloudDocWsEventVO event) {
        ConcurrentHashMap<String, SessionState> docRoom = roomSessions.get(docId);
        if (docRoom == null || docRoom.isEmpty()) {
            return;
        }
        Set<String> accounts = new HashSet<>();
        for (SessionState state : docRoom.values()) {
            accounts.add(state.account);
        }
        for (String account : accounts) {
            messagingTemplate.convertAndSendToUser(account, "/queue/cloud-docs.events", event);
        }
    }

    private void removeSessionFromDoc(String sessionId, String docId) {
        ConcurrentHashMap<String, SessionState> docRoom = roomSessions.get(docId);
        if (docRoom != null) {
            docRoom.remove(sessionId);
            if (docRoom.isEmpty()) {
                roomSessions.remove(docId);
            }
        }
        Set<String> docs = sessionDocs.get(sessionId);
        if (docs != null) {
            docs.remove(docId);
            if (docs.isEmpty()) {
                sessionDocs.remove(sessionId);
            }
        }
    }

    private void ensureAccess(String account, String docId) {
        CloudDocEntity doc = cloudDocRepository.findByDocIdAndDeletedFalse(docId)
                .orElseThrow(() -> new IllegalStateException("文档不存在"));
        if (account.equals(doc.getOwnerAccount())) {
            return;
        }
        List<CloudDocShareEntity> shares = cloudDocShareRepository
                .findByDocIdAndFriendAccountAndStatusAndShareMode(docId, account, "ACTIVE", SHARE_MODE_COLLAB);
        LocalDateTime now = LocalDateTime.now();
        boolean allowed = shares.stream().anyMatch(share -> share.getExpireAt() == null || now.isBefore(share.getExpireAt()));
        if (!allowed) {
            throw new IllegalStateException("无权限协作该文档");
        }
    }

    private String normalizeDocId(String docIdRaw) {
        String docId = trim(docIdRaw);
        if (docId == null) {
            throw new IllegalArgumentException("docId 不能为空");
        }
        return docId;
    }

    private Integer normalizeCursor(Integer value) {
        if (value == null || value < 0) {
            return 0;
        }
        return value;
    }

    private CloudDocWsAckVO ack(String action, String docId) {
        return CloudDocWsAckVO.builder()
                .action(action)
                .docId(docId)
                .status("SENT")
                .at(formatUtc(LocalDateTime.now()))
                .build();
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

    private static class SessionState {
        private final String account;
        private Integer cursorAnchor;
        private Integer cursorHead;

        private SessionState(String account) {
            this.account = account;
            this.cursorAnchor = 0;
            this.cursorHead = 0;
        }
    }
}
