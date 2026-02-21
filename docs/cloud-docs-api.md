# 云文档 API 文档

## 1. 统一约定
- 接口前缀：`/cloud-docs`
- 鉴权方式：`Authorization: Bearer <accessToken>`
- 返回结构：`{ code, status, message, data }`
- 时间格式：ISO-8601 UTC（示例：`2026-02-20T10:30:00Z`）
- 分页结构：`records/page/size/total/totalPages/hasMore`

## 2. 数据模型

### 2.1 CloudDocSummary
```json
{
  "id": "doc_20260220_000001",
  "title": "未标题云文档",
  "snippet": "这里是文档摘要内容...",
  "createdAt": "2026-02-20T09:30:00Z",
  "updatedAt": "2026-02-20T10:30:00Z",
  "lastSavedAt": "2026-02-20T10:30:00Z",
  "version": 8,
  "deleted": false
}
```

### 2.2 CloudDocDetail
```json
{
  "id": "doc_20260220_000001",
  "title": "需求评审记录",
  "contentHtml": "<p>文档内容</p>",
  "contentJson": "{\"type\":\"doc\",\"content\":[]}",
  "createdAt": "2026-02-20T09:30:00Z",
  "updatedAt": "2026-02-20T10:30:00Z",
  "lastSavedAt": "2026-02-20T10:30:00Z",
  "version": 8,
  "ownerAccount": "10001",
  "editable": true
}
```

### 2.3 CloudDocSaveRequest
```json
{
  "title": "需求评审记录",
  "contentHtml": "<p>文档内容</p>",
  "contentJson": "{\"type\":\"doc\",\"content\":[]}",
  "baseVersion": 8
}
```

### 2.4 CloudDocSaveResponse
```json
{
  "id": "doc_20260220_000001",
  "updatedAt": "2026-02-20T10:31:00Z",
  "lastSavedAt": "2026-02-20T10:31:00Z",
  "version": 9
}
```

## 3. P0 接口

### 3.1 查询云文档列表
- 方法：`GET /cloud-docs?page=1&size=20&keyword=需求&sort=updatedAt_desc`
- Query：
- `page` 默认 `1`
- `size` 默认 `20`，最大 `100`
- `keyword` 可选，按标题搜索
- `sort` 可选，默认 `updatedAt_desc`
- 成功响应（200）：
```json
{
  "code": 200,
  "status": "OK",
  "message": "success",
  "data": {
    "records": [],
    "page": 1,
    "size": 20,
    "total": 0,
    "totalPages": 0,
    "hasMore": false
  }
}
```

### 3.2 新建云文档
- 方法：`POST /cloud-docs`
- 请求体（可选）：
```json
{
  "title": "未标题云文档"
}
```
- 说明：不传 `title` 时，服务端默认值为“未标题云文档”。
- 成功响应（200）：`data` 为 `CloudDocDetail`

### 3.3 获取云文档详情
- 方法：`GET /cloud-docs/{docId}`
- 用途：打开编辑器时一次获取完整内容
- 成功响应（200）：`data` 为 `CloudDocDetail`
- 失败响应（404）：`CLOUD_DOC_NOT_FOUND`

### 3.4 保存云文档（自动保存）
- 方法：`PUT /cloud-docs/{docId}`
- 请求体：`CloudDocSaveRequest`
- 成功响应（200）：`data` 为 `CloudDocSaveResponse`
- 版本规则（当前实现）：
- 当 `baseVersion < 服务端DB当前version` 时返回 `409`。
- 当 `baseVersion >= 服务端DB当前version` 时允许保存（用于降低协同线短暂漂移导致的误冲突）。
- 冲突响应（409）：
```json
{
  "code": 409,
  "status": "CONFLICT",
  "message": "版本冲突",
  "data": {
    "latestVersion": 10,
    "latestUpdatedAt": "2026-02-20T10:32:00Z"
  },
  "errorCode": "CLOUD_DOC_VERSION_CONFLICT"
}
```

### 3.5 删除云文档
- 方法：`DELETE /cloud-docs/{docId}`
- 语义：建议逻辑删除，接口保持幂等
- 成功响应（200）：
```json
{
  "code": 200,
  "status": "OK",
  "message": "success",
  "data": {}
}
```

### 3.6 上传图片
- 方法：`POST /files/upload`
- Content-Type：`multipart/form-data`
- 字段：`file`
- 成功响应（200）示例：
```json
{
  "code": 200,
  "status": "success",
  "message": "上传成功",
  "data": {
    "url": "https://api.example.com/files/image/xx.png",
    "objectName": "xx.png",
    "width": 1200,
    "height": 800,
    "size": 102400
  }
}
```

## 4. P1 接口（建议）
- `GET /cloud-docs/{docId}/revisions`：历史版本列表
- `POST /cloud-docs/{docId}/restore`：按版本回滚
- `POST /cloud-docs/{docId}/duplicate`：复制文档
- `POST /cloud-docs/{docId}/share`：分享给好友查看（已实现）
- `GET /cloud-docs/shares/{shareNo}`：通过分享号查看文档（已实现）
- `DELETE /cloud-docs/shares/{shareNo}`：撤销分享（已实现）
- `GET /cloud-docs/shares/received`：我收到的分享列表（已实现）

### 4.1 分享给好友查看
- 方法：`POST /cloud-docs/{docId}/share`
- 请求体：
```json
{
  "friendAccount": "10002",
  "expireHours": 168,
  "shareMode": "READONLY"
}
```
- 规则：
- 仅文档 owner 可分享。
- 仅可分享给好友关系为 `ACCEPTED` 的用户。
- `expireHours` 可选，范围 `1~720`，默认 `168`（7 天）。
- `shareMode` 可选：`READONLY | COLLAB`，默认 `READONLY`。
- 只有 `COLLAB` 分享允许接入 WS 协作协议。
- 成功响应（200）：
```json
{
  "code": 200,
  "status": "success",
  "message": "分享云文档成功",
  "data": {
    "shareNo": "share_20260220_abc123def0",
    "docId": "doc_20260220_000001",
    "friendAccount": "10002",
    "shareMode": "READONLY",
    "createdAt": "2026-02-20T12:00:00Z",
    "expireAt": "2026-02-27T12:00:00Z",
    "sharePath": "/cloud-docs/shares/share_20260220_abc123def0"
  }
}
```

### 4.2 查看分享文档
- 方法：`GET /cloud-docs/shares/{shareNo}`
- 规则：
- 仅分享接收方（`friendAccount`）和分享发起人（`ownerAccount`）可查看。
- `READONLY` 分享：接收方只读（`editable=false`）。
- `COLLAB` 分享：接收方可编辑（`editable=true`），并可接入 WS 协作。
- 成功响应（200）：
```json
{
  "code": 200,
  "status": "success",
  "message": "查询分享文档成功",
  "data": {
    "shareNo": "share_20260220_abc123def0",
    "shareMode": "COLLAB",
    "collaborative": true,
    "doc": {
      "id": "doc_20260220_000001",
      "title": "需求评审记录",
      "contentHtml": "<p>文档内容</p>",
      "contentJson": "{\"type\":\"doc\",\"content\":[]}",
      "createdAt": "2026-02-20T09:30:00Z",
      "updatedAt": "2026-02-20T10:30:00Z",
      "lastSavedAt": "2026-02-20T10:30:00Z",
      "version": 8,
      "ownerAccount": "10001",
      "editable": true
    }
  }
}
```

### 4.5 分享模式权限矩阵
- `READONLY`：
- 可查看：是
- 可调用 `PUT /cloud-docs/{docId}` 编辑：否
- 可接入 `/app/cloud-docs.*` 协作 WS：否
- `COLLAB`：
- 可查看：是
- 可调用 `PUT /cloud-docs/{docId}` 编辑：是
- 可接入 `/app/cloud-docs.*` 协作 WS：是

### 4.3 撤销分享
- 方法：`DELETE /cloud-docs/shares/{shareNo}`
- 规则：
- 仅分享发起人（`ownerAccount`）可撤销。
- 已撤销分享重复操作返回成功（幂等）。
- 成功响应（200）：
```json
{
  "code": 200,
  "status": "success",
  "message": "撤销分享成功",
  "data": {
    "shareNo": "share_20260220_abc123def0",
    "revoked": true,
    "revokedAt": "2026-02-20T12:30:00Z"
  }
}
```

### 4.4 我收到的分享列表
- 方法：`GET /cloud-docs/shares/received?page=1&size=20&status=ACTIVE`
- Query：
- `page` 默认 `1`
- `size` 默认 `20`，最大 `100`
- `status` 可选：`ACTIVE | EXPIRED | REVOKED`
- 成功响应（200）：
```json
{
  "code": 200,
  "status": "success",
  "message": "查询我收到的分享成功",
  "data": {
    "records": [
      {
        "shareNo": "share_20260220_abc123def0",
        "docId": "doc_20260220_000001",
        "title": "需求评审记录",
        "snippet": "这里是摘要",
        "ownerAccount": "10001",
        "shareMode": "READONLY",
        "status": "ACTIVE",
        "expired": false,
        "createdAt": "2026-02-20T12:00:00Z",
        "expireAt": "2026-02-27T12:00:00Z",
        "lastViewedAt": "2026-02-20T12:10:00Z"
      }
    ],
    "page": 1,
    "size": 20,
    "total": 1,
    "totalPages": 1,
    "hasMore": false
  }
}
```

## 5. P2 接口（协作扩展）
- `GET /cloud-docs/{docId}/presence`：在线成员与游标
- `WS /ws` + STOMP：协作事件推送（内容变更/游标/在线状态，已实现）
- 详细协议见：`/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/docs/websocket-cloud-docs-api.md`
- 前端接入建议见：`/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/docs/cloud-docs-collab-frontend-guide.md`

## 6. 错误码
- `400 + CLOUD_DOC_INVALID_PARAM`：参数非法
- `401 + UNAUTHORIZED`：未登录或 token 无效
- `403 + CLOUD_DOC_FORBIDDEN`：无权限操作
- `404 + CLOUD_DOC_NOT_FOUND`：文档不存在
- `409 + CLOUD_DOC_VERSION_CONFLICT`：版本冲突
- `413 + FILE_TOO_LARGE`：上传文件过大
- `415 + FILE_TYPE_NOT_ALLOWED`：上传文件类型不支持
- `500 + INTERNAL_ERROR`：服务端异常
