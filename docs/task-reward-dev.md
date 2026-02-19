# 任务奖励开发文档

## 1. 方案概览
- 触发方式：
  - 外部调用 `POST /task-events`
  - 系统内自动触发：`MomentServiceImpl#createMoment/createComment` 成功后调用任务服务
- 发奖模型：事件日志 + 发奖记录 + 账本流水 + 账户余额/成长值
- 一致性：单事务提交（`@Transactional`），同事务内完成事件记录、发奖记录、账户更新与流水。

## 2. 数据模型
新增表：
- `task_config`
- `task_event`
- `reward_grant`
- `daily_counter`
- `wallet_ledger`
- `growth_account`
- `growth_ledger`

复用并扩展：
- `wallet_account` 新增字段：
  - `balance_cent bigint`
  - `reward_version int`

说明：项目原有钱包为 `decimal`，为兼容旧接口保留 `balance`；任务奖励使用 `balance_cent` 作为主口径，同时同步更新 `balance`。

## 3. 任务规则实现
### 3.1 发帖奖励（POST_CREATE）
- 条件：`isDraft=false && isDeleted=false && isBlocked=false`
- 限额：自然日最多 3 次
- 奖励：每次 `+500` 分（`+5.00` 元）
- 超限：`SKIPPED_LIMIT`，不发奖但记录 `task_event/reward_grant`

### 3.2 回复奖励（REPLY_CREATE）
- 条件：`isDeleted=false && isBlocked=false && isSystemBackfill=false`
- 奖励：每次 `+15` 成长值
- 风控：
  - 同用户同帖子最小奖励间隔（默认 10 秒）
  - 同用户同帖子每分钟最多奖励次数（默认 5）
  - 可通过 `task_config.risk_policy_json` 调整

## 4. 幂等与并发
### 4.1 幂等
- `task_event.event_id` 唯一索引
- `reward_grant.event_id` 唯一索引
- `wallet_ledger.event_id` / `growth_ledger.event_id` 唯一索引
- 重复请求返回 409 + 首次处理结果

### 4.2 并发
- 发帖日计数：`daily_counter(user_id, task_type, biz_date)` 唯一 + `SELECT ... FOR UPDATE`
- 钱包余额：`wallet_account` 行锁 (`findByUserIdForUpdate`)
- 成长值账户：`growth_account` 行锁 (`findByUserIdForUpdate`)
- 用户成长值同步：`user_info_test` 行锁 (`UserRepository#findByIdForUpdate`)

## 5. 关键代码路径
- 控制器：`src/main/java/com/lujianfeng/spanner/controller/TaskRewardController.java`
- 服务：`src/main/java/com/lujianfeng/spanner/service/impl/TaskRewardServiceImpl.java`
- 接口：`src/main/java/com/lujianfeng/spanner/service/task/TaskRewardService.java`
- 自动触发接入：`src/main/java/com/lujianfeng/spanner/service/impl/MomentServiceImpl.java`

## 6. 验收用例清单
1. 首次 `POST_CREATE`，余额 +500 分，`daily_counter=1`
2. 同日第 2、3 次 `POST_CREATE` 均发奖
3. 同日第 4 次 `POST_CREATE` 返回 `SKIPPED_LIMIT`
4. 超限事件必须写入 `task_event` 与 `reward_grant`
5. 跨天后再次 `POST_CREATE` 可继续发奖
6. 草稿/删除/屏蔽发帖事件不发奖，状态 `SKIPPED_INVALID`
7. 有效 `REPLY_CREATE` 成长值 +15
8. 系统回填/删除/屏蔽回复不发奖，状态 `SKIPPED_INVALID`
9. 同一 `eventId` 重试 10 次，仅首条入账，其余返回重复结果
10. 高并发 20 次发帖事件同日最多发奖 3 次
11. 同帖 10 秒内连续回复触发 `RISK_REPLY_TOO_FREQUENT`
12. 查询流水与账户汇总一致（`ledger` 累积等于账户值）
