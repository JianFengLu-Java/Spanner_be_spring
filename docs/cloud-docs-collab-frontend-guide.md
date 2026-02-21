# 云文档协同前端接入指南（适配当前后端可用协同内核）

## 1. 目标
- 在多人编辑时降低并发冲突。
- 保证本地编辑不中断，服务端版本线可追踪。
- 统一处理 WS 实时协同与 REST 持久化。

## 2. 先决条件
- 分享模式必须是 `COLLAB` 才开启协同。
- `READONLY` 仅查看，不连接协同 WS。
- 你需要同时使用：
- REST：`GET /cloud-docs/{docId}`、`PUT /cloud-docs/{docId}`
- WS：`/app/cloud-docs.join|leave|cursor|patch`

## 3. 前端状态模型（建议）
每个文档维护：
- `serverVersion`: number（当前服务端版本，初始化为文档详情 `version`）
- `pendingOps`: Op[]（已发未确认）
- `ackedOps`: Set<opId>
- `connected`: boolean
- `inCollabMode`: boolean（由 shareMode 决定）

Op 结构建议：
```ts
interface CollabOp {
  opId: string
  baseVersion: number
  opType: string
  payload: string
  createdAt: number
}
```

## 4. 连接与入场流程
1. 打开文档后先调 `GET /cloud-docs/{docId}`。
2. 若 `editable=false` 且 `shareMode!=COLLAB`：只读模式，不连协同。
3. 若可协同：
- 建立 STOMP 连接；
- 订阅 `/user/queue/cloud-docs.events`、`/user/queue/cloud-docs.acks`、`/user/queue/errors`；
- 发送 `/app/cloud-docs.join`。
4. 收到 `presence.snapshot` 后，用 `data.serverVersion` 覆盖本地 `serverVersion`。

## 5. Patch 发送规则（关键）
发送 patch 时：
1. 先生成 `opId`。
2. `baseVersion` 必须等于当前本地 `serverVersion`。
3. 将 op 放入 `pendingOps`。
4. 发送 `/app/cloud-docs.patch`。

示例：
```json
{
  "docId": "doc_20260220_000001",
  "baseVersion": 12,
  "opId": "op_abc123",
  "opType": "replace",
  "payload": "{...}"
}
```

## 6. ACK 状态机处理
`/user/queue/cloud-docs.acks` 返回：
- `APPLIED`：
- 更新 `serverVersion = ack.serverVersion`。
- 从 `pendingOps` 移除该 `opId`。
- `CONFLICT`：
- 立即停止继续发送新 op（进入 `resync`）。
- 走“重同步流程”（见第 8 节）。
- `DUPLICATE`：
- 从 `pendingOps` 移除即可。
- `REJECTED`：
- 记录错误并提示，必要时重同步。

## 7. 接收他人 patch
收到 `content.patch` 事件：
1. 检查 `data.serverVersion`。
2. 若 `data.serverVersion == local.serverVersion + 1`：按序应用并更新 `serverVersion`。
3. 若出现跳号（例如本地 12，收到 15）：进入 `resync`。

## 8. 重同步流程（必须实现）
触发条件：
- patch ack 为 `CONFLICT`
- 收到 patch 跳号
- 重连后版本不连续

流程：
1. 暂停发送 patch。
2. 调 `GET /cloud-docs/{docId}` 拉取最新全文。
3. 用最新内容覆盖编辑器文档树。
4. 清空 `pendingOps`（或按业务需要重算未提交输入）。
5. `serverVersion = detail.version`。
6. 恢复发送。

## 9. 保存策略（与协同版本线对齐）
建议：
- 不要每个按键都 `PUT`。
- 使用“空闲保存”：编辑停顿 2-5 秒触发一次 `PUT`。
- 保存请求 `baseVersion` 必须使用当前 `serverVersion`。

保存成功：
- 以 `PUT` 响应中的 `version` 覆盖本地 `serverVersion`。

保存 `409`：
- 走重同步流程。

## 10. 光标与在线成员
- 本地光标变化节流发送（建议 80~150ms）。
- `presence.snapshot` 全量覆盖在线成员列表。
- `presence.join/leave` 作为增量提示，可不单独维护复杂状态。

## 11. 断线重连
重连后：
1. 自动重新订阅。
2. 重新发送 `join`。
3. 立即拉一次 `GET /cloud-docs/{docId}` 做保险同步。
4. 重置 `pendingOps`。

## 12. 建议的容错与监控
- 记录以下指标：
- `ack.CONFLICT` 次数
- `resync` 次数
- `PUT 409` 次数
- `ws reconnect` 次数
- 当 `CONFLICT` 或 `409` 占比异常升高时，提示用户“协同繁忙，已自动同步”。

## 13. 最小前端伪代码
```ts
onEditorChange(delta) {
  if (!inCollabMode) return
  const op: CollabOp = {
    opId: uuid(),
    baseVersion: state.serverVersion,
    opType: 'replace',
    payload: JSON.stringify(delta),
    createdAt: Date.now()
  }
  state.pendingOps.push(op)
  ws.publish('/app/cloud-docs.patch', op)
}

onAck(ack) {
  if (ack.status === 'APPLIED') {
    state.serverVersion = ack.serverVersion
    removePending(ack.opId)
    return
  }
  if (ack.status === 'CONFLICT') {
    resync()
  }
}

async function resync() {
  pauseSending = true
  const detail = await api.getDoc(docId)
  editor.replaceDoc(detail.contentJson)
  state.serverVersion = detail.version
  state.pendingOps = []
  pauseSending = false
}
```

## 14. 参考文档
- REST API：`/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/docs/cloud-docs-api.md`
- WS 协议：`/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/docs/websocket-cloud-docs-api.md`
