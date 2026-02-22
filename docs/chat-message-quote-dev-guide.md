# 消息引用功能开发文档细则

## 1. 设计目标

1. 支持在“发送消息”时携带被引用消息信息
2. 保证实时下发、离线拉取、历史查询三条链路字段一致
3. 保持对旧客户端兼容（`quote` 可选）

---

## 2. 后端实现范围

已实现模块：
1. 输入模型：`PrivateMessageSendDTO`、`GroupMessageSendDTO` 新增 `quote`
2. 下行模型：`PrivateMessageVO`、`GroupMessageVO` 新增 `quote`
3. 存储模型：`private_message`、`group_message` 新增引用字段
4. 分发服务：私聊/群聊发送链路支持引用透传与入库
5. 查询链路：私聊离线、私聊历史、群聊历史返回 `quote`

---

## 3. 数据库字段

新增字段（由 JPA `ddl-auto=update` 自动建列）：

私聊表 `private_message`：
1. `quoted_message_id` varchar(64) null
2. `quoted_from_account` varchar(32) null
3. `quoted_content` varchar(2000) null

群聊表 `group_message`：
1. `quoted_message_id` varchar(64) null
2. `quoted_from_account` varchar(32) null
3. `quoted_content` varchar(2000) null

说明：
1. 当前按“快照存储”策略，引用内容冗余保存，避免原消息变更影响展示
2. 未强制外键，兼容跨端/跨会话引用快照场景

---

## 4. 校验与错误处理

校验规则：
1. `quote` 可为空
2. `quote` 非空时，`quote.messageId` 必填

错误码（WebSocket 错误通道）：
1. `INVALID_PARAM`：引用参数不合法（如缺失 `quote.messageId`）

---

## 5. 关键类与职责

1. `src/main/java/com/lujianfeng/spanner/dto/message/MessageQuoteDTO.java`
   发送请求中的引用结构
2. `src/main/java/com/lujianfeng/spanner/vo/message/MessageQuoteVO.java`
   下行与查询统一引用结构
3. `src/main/java/com/lujianfeng/spanner/controller/ChatController.java`
   解析/校验 `quote` 并传给分发服务
4. `src/main/java/com/lujianfeng/spanner/service/PrivateMessageDispatchService.java`
   私聊实时分发 + 入库引用字段
5. `src/main/java/com/lujianfeng/spanner/service/GroupMessageDispatchService.java`
   群聊实时分发 + 入库引用字段
6. `src/main/java/com/lujianfeng/spanner/controller/OfflineMessageController.java`
   私聊历史返回 `quote`
7. `src/main/java/com/lujianfeng/spanner/controller/GroupController.java`
   群聊历史返回 `quote`
8. `src/main/java/com/lujianfeng/spanner/service/OfflineMessageService.java`
   离线消息反序列化时兼容 `quote`

---

## 6. 联调清单

1. 私聊发送带 `quote`，发送方与接收方实时收到 `quote`
2. 群聊发送带 `quote`，群成员实时收到 `quote`
3. 断线后拉取 `/messages/offline`，`quote` 不丢失
4. 拉取 `/messages/history/{friendAccount}`，`quote` 与实时消息一致
5. 拉取 `/groups/{groupNo}/messages/history`，`quote` 与实时消息一致
6. 不带 `quote` 的老消息流程无回归

---

## 7. 后续可选增强

1. 引用消息存在性校验（按会话范围校验 messageId）
2. 引用内容服务端统一摘要，减少客户端处理复杂度
3. 引用消息撤回后的降级展示策略（如“该消息已删除”）
