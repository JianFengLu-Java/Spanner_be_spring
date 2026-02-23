# 音视频通话模块开发文档

## 1. 目标
- 交付 1v1 音视频通话后端能力，覆盖发起、接听、拒绝、取消、挂断、信令交换、超时结束。
- 对接现有 STOMP 通道，支持 `incoming.call`、`call.answered`、`call.ended`、`call.signal` 推送。
- 保持与现有返回结构一致：`{ code, status, message, data }`。

## 2. 代码落地
- Controller: `src/main/java/com/lujianfeng/spanner/controller/CallController.java`
- Service:
- `src/main/java/com/lujianfeng/spanner/service/CallService.java`
- `src/main/java/com/lujianfeng/spanner/service/impl/CallServiceImpl.java`
- Entity:
- `src/main/java/com/lujianfeng/spanner/entity/call/CallSessionEntity.java`
- `src/main/java/com/lujianfeng/spanner/entity/call/CallRequestLogEntity.java`
- `src/main/java/com/lujianfeng/spanner/entity/call/CallStatusEnum.java`
- `src/main/java/com/lujianfeng/spanner/entity/call/CallTypeEnum.java`
- `src/main/java/com/lujianfeng/spanner/entity/call/CallSignalTypeEnum.java`
- Repository:
- `src/main/java/com/lujianfeng/spanner/repository/CallSessionRepository.java`
- `src/main/java/com/lujianfeng/spanner/repository/CallRequestLogRepository.java`

## 3. 核心设计

### 3.1 状态机
- `RINGING -> ANSWERED|REJECTED|CANCELED|NO_ANSWER|BUSY`
- `ANSWERED -> CONNECTING -> CONNECTED|FAILED`
- `CONNECTED -> ENDED`
- 终态：`REJECTED|CANCELED|NO_ANSWER|BUSY|ENDED|FAILED`

### 3.2 幂等与并发
- 所有写操作依赖 `requestId`，服务端写入 `call_request_log`。
- 同一 `requestId` 重试会返回已处理结果，不重复改状态。
- 会话更新使用 `PESSIMISTIC_WRITE` 行锁，避免并发状态覆盖。

### 3.3 忙线策略
- 同一账号同一时刻仅允许一个活跃会话：`RINGING|ANSWERED|CONNECTING|CONNECTED`。
- 发起通话时如果被叫存在活跃会话，返回 `409 CALLEE_BUSY`。

### 3.4 事件推送
- 呼叫事件通道：`/user/queue/calls`
- 信令事件通道：`/user/queue/call-signals`
- 通过 `SimpMessagingTemplate.convertAndSendToUser` 推送到账号绑定的 STOMP 用户队列。

### 3.5 超时处理
- `SpannerApplication` 启用 `@EnableScheduling`。
- `CallServiceImpl#markNoAnswerTimeoutSessions` 每 3 秒扫描过期 `RINGING` 会话并落为 `NO_ANSWER`，同步推送 `call.ended`。

## 4. 数据表

### 4.1 `call_session`
- 会话主表，主键 `id`，业务唯一键 `call_id`。
- 关键字段：`type/status/caller_account/callee_account/started_at/expires_at/answered_at/connected_at/ended_at/end_reason/duration_seconds`。

### 4.2 `call_request_log`
- 幂等日志表，唯一键 `request_id`。
- 关键字段：`request_id/call_id/actor_account/action/result_status/created_at`。

## 5. 关键业务规则
- 仅通话双方可操作会话。
- 仅被叫可接听/拒绝，主叫可取消。
- 通话中任意一方可挂断。
- 信令 `to` 必须是当前会话对端。
- `OFFER/ANSWER` 必须带 `sdp`，`ICE_CANDIDATE` 必须带 `candidate`。

## 6. 已实现接口范围
- P0 全量：创建/接听/拒绝/取消/挂断/详情/信令/WS 事件。
- P1 部分：`/calls/history`、`/calls/{callId}/heartbeat`、`/rtc/ice-servers`（当前默认下发 STUN）。

## 7. 联调说明
- 前端需在 STOMP `CONNECT` 帧携带 JWT（`Authorization: Bearer <token>`）。
- 订阅：
- `/user/queue/calls`
- `/user/queue/call-signals`
- 信令 API 与 WS 并行使用：客户端上行 `POST /calls/{callId}/signals`，服务端下行 `call.signal`。
