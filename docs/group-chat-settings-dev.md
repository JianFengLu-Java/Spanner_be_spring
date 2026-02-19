# 群聊设置页后端开发文档

## 1. 本次交付范围
- 按前端需求完成群设置页 P0/P1 接口实现。
- 保持现有群聊接口兼容，不破坏已有联调功能。
- 统一返回结构 `code/status/message/data`。

## 2. 代码改动说明

### 2.1 控制器
- 文件：`/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/src/main/java/com/lujianfeng/spanner/controller/GroupController.java`
- 新增能力：
- 群资料查询/编辑
- 群设置详情聚合
- 群成员分页、预览
- 我的设置更新
- 群权限配置更新
- 公告最新/发布/编辑/历史
- 批量邀请、成员角色更新、成员移除别名
- 媒体统计、清空消息
- 退出群聊增强返回、解散群聊、举报

### 2.2 服务层
- 文件：`/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/src/main/java/com/lujianfeng/spanner/service/ChatGroupService.java`
- 增加群设置领域逻辑：
- `getGroupSettingsDetail`
- `listGroupMembersPage` / `listGroupMembersPreview`
- `updateMySettings`
- `updateGroupPermissions`
- `createAnnouncement` / `updateAnnouncementById` / `listAnnouncements`
- `batchInvite`
- `updateMemberRole`
- `getMediaOverview`
- `clearGroupMessages`
- `quitGroupWithResult`
- `dissolveGroup`
- `reportGroup`

## 3. 数据模型扩展

### 3.1 `chat_group` 扩展字段
- `group_avatar_url`
- `summary`
- `invite_mode`（`ALL | ADMIN_ONLY`）
- `member_can_edit_group_name`
- `join_verification_enabled`
- `announcement_permission`（`OWNER_ONLY | OWNER_ADMIN`）

### 3.2 新增实体表
- `chat_group_profile`：群资料实体对象（群头像、群简介）
- `chat_group_announcement`：公告历史
- `chat_group_user_settings`：用户在群内个性化设置
- `chat_group_report`：群举报记录

## 4. 关键业务规则
- 权限：
- 群权限配置：`OWNER | ADMIN`
- 成员角色更新：仅 `OWNER`
- 解散群聊：仅 `OWNER`
- 群主退群：禁止，返回冲突
- 批量邀请：遵循群邀请模式；失败明细返回 `reasonCode`
- 公告文本、举报描述做长度校验并进行基础标签过滤
- 危险操作（踢人、角色变更、退群、解散）写审计日志

## 5. 分页与返回
- 成员/公告列表统一输出：
- `records`
- `page`
- `size`
- `total`
- `totalPages`
- `hasMore`

## 6. 编译验证
- 执行：`./mvnw -q -DskipTests compile`
- 结果：通过。

## 7. 后续建议
- 将时间字段统一升级为 UTC 带时区类型（如 `OffsetDateTime`）以完全对齐前端文档。
- 举报接口可补充 Redis 限流与内容安全检测。
- 成员 `muted/blacklisted/status` 可接入独立表和在线状态缓存实现真实值。
