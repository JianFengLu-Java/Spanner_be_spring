# 云文档后端开发文档

## 1. 目标与范围
- 对齐前端云文档组件联调需求，交付 P0 能力：列表、创建、详情、保存、删除、图片上传。
- 保存接口支持高频自动保存与乐观锁并发控制。
- 为 P1/P2（历史版本、协作）预留可扩展模型。

## 2. 当前代码现状
- 项目已具备图片上传能力：`POST /files/upload`，返回可访问 `url`。
- 当前仓库尚无独立云文档领域代码（Controller/Service/Repository/Entity 均未落地）。
- 本文档作为云文档功能实现基线，供后续开发与联调使用。

## 3. 分层设计

### 3.1 Controller（建议新增）
- `CloudDocController`
- 路由：
- `GET /cloud-docs`
- `POST /cloud-docs`
- `GET /cloud-docs/{docId}`
- `PUT /cloud-docs/{docId}`
- `DELETE /cloud-docs/{docId}`

### 3.2 Service（建议新增）
- `CloudDocService`
- 核心方法：
- `listMyDocs(page, size, keyword, sort)`
- `createDoc(account, title)`
- `getDocDetail(account, docId)`
- `saveDoc(account, docId, request)`
- `deleteDoc(account, docId)`

### 3.3 Repository（建议新增）
- `CloudDocRepository`
- `CloudDocRevisionRepository`（P1）
- 查询需默认过滤 `deleted=false`，并按 `ownerAccount` 做数据隔离。

## 4. 数据模型建议

### 4.1 主表 `cloud_doc`
- `id`：文档 ID（字符串或雪花主键）
- `owner_account`：所属账号
- `title`：标题
- `snippet`：摘要（可由 `contentHtml` 生成）
- `content_html`：HTML 内容
- `content_json`：Tiptap JSON 字符串
- `version`：乐观锁版本号（初始 1，保存 +1）
- `deleted`：逻辑删除标记
- `created_at`、`updated_at`、`last_saved_at`

索引建议：
- `idx_cloud_doc_owner_updated(owner_account, updated_at desc)`
- `idx_cloud_doc_owner_deleted(owner_account, deleted)`
- `uk_cloud_doc_id(id)`（唯一）

### 4.2 历史版本表 `cloud_doc_revision`（P1）
- `id`
- `doc_id`
- `version`
- `title`
- `content_html`
- `content_json`
- `created_at`

索引建议：
- `idx_revision_doc_version(doc_id, version desc)`

## 5. 关键流程

### 5.1 新建文档
1. 生成文档 ID，默认标题“未标题云文档”。
2. 初始化空内容与 `version=1`。
3. 返回 `CloudDocDetail`，前端可直接进入编辑。

### 5.2 自动保存（乐观锁）
1. 校验 `baseVersion` 必填。
2. 根据 `docId + ownerAccount` 查询文档。
3. 比较 `baseVersion` 与当前 `version`：
- 一致：更新内容，`version = version + 1`。
- 不一致：返回 `409 + CLOUD_DOC_VERSION_CONFLICT`，并返回 `latestVersion/latestUpdatedAt`。
4. 更新 `updatedAt/lastSavedAt`，返回 `CloudDocSaveResponse`。

### 5.3 删除文档
1. 按 `docId + ownerAccount` 执行逻辑删除。
2. 已删除再次删除保持幂等，返回成功。

## 6. 安全与校验
- 所有接口基于登录态，服务端按当前账号强制过滤文档归属。
- `PUT/DELETE` 仅允许文档所有者操作。
- `title/contentHtml/contentJson` 做长度与空值校验。
- `contentHtml` 入库前做基础 XSS 过滤。
- 上传文件限制：仅图片类型，大小 <= 10MB。

## 7. 错误码与异常映射
- `CLOUD_DOC_INVALID_PARAM` -> `400`
- `UNAUTHORIZED` -> `401`
- `CLOUD_DOC_FORBIDDEN` -> `403`
- `CLOUD_DOC_NOT_FOUND` -> `404`
- `CLOUD_DOC_VERSION_CONFLICT` -> `409`
- `INTERNAL_ERROR` -> `500`

## 8. 联调与验收清单
- 前端可完成创建、列表、详情打开、自动保存、删除闭环。
- 高频保存场景不丢数据，冲突返回可恢复信息。
- 列表中的 `updatedAt/lastSavedAt/version` 与保存结果一致。
- 上传图片后可直接在编辑器渲染。
- 时间字段统一 ISO-8601 UTC，分页字段完整一致。

## 9. 建议落地顺序
1. P0：`cloud_doc` 表 + CRUD + 乐观锁保存 + 权限校验。
2. P1：`cloud_doc_revision` + 恢复/复制/分享。
3. P2：WebSocket 协作（presence/游标/变更广播）。
