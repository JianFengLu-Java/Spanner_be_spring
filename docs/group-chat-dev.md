# 群聊模块开发文档

## 1. 设计目标
- 在现有私聊基础上扩展群聊能力。
- 每个群有唯一群号，支持最多 500 人。
- 支持群主、管理员、普通成员角色。
- 支持群公告和群消息历史查询。

## 2. 核心表结构

### 2.1 `chat_group`
- `group_no`：群号，唯一索引
- `group_name`：群名称
- `owner_account`：群主账号
- `announcement`：群公告
- `max_members`：最大人数（固定 500）
- `created_at`、`updated_at`

### 2.2 `chat_group_member`
- `group_id`
- `user_account`
- `role`：`OWNER` / `ADMIN` / `MEMBER`
- `joined_at`
- 唯一约束：`(group_id, user_account)`

### 2.3 `group_message`
- `message_id`：消息唯一 ID
- `group_no`
- `from_account`
- `content`
- `client_message_id`
- `sent_at`

## 3. 角色权限
- `OWNER`
- 设置/取消管理员
- 修改公告
- 踢人
- `ADMIN`
- 修改公告
- 踢普通成员（不能踢管理员/群主）
- `MEMBER`
- 收发群消息
- 查看群资料与成员

## 4. 关键业务规则
- 入群时校验人数上限（500）。
- 邀请入群要求：邀请人与被邀请人必须是好友关系（`ACCEPTED`）。
- 群主暂不支持退群（防止群无主）。
- 群消息仅允许群成员发送。
- 群消息通过 WebSocket 下发到群成员个人队列：
- `/user/queue/group.messages`

## 5. 主要代码位置
- 群组服务：`/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/src/main/java/com/lujianfeng/spanner/service/ChatGroupService.java`
- 群消息分发：`/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/src/main/java/com/lujianfeng/spanner/service/GroupMessageDispatchService.java`
- REST 控制器：`/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/src/main/java/com/lujianfeng/spanner/controller/GroupController.java`
- WebSocket 控制器：`/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/src/main/java/com/lujianfeng/spanner/controller/ChatController.java`

## 6. 后续可扩展建议
- 增加群主转让接口（支持群主退群）。
- 增加入群申请/审核机制（目前为直接加入）。
- 增加群消息离线拉取与未读计数。
