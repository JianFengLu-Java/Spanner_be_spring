# 用户钱包账户开发文档

## 1. 功能目标
为已登录用户提供钱包账户能力，支持以下基础场景：
- 自动开户（用户注册后自动创建钱包）
- 查询当前钱包信息（余额、币种、状态）
- 钱包充值（余额增加）
- 钱包消费（余额扣减）

## 2. 设计说明

### 2.1 数据模型
新增实体：`wallet_account`、`wallet_flow`

字段说明：
- `id`: 主键，自增
- `wallet_no`: 钱包号，唯一（当前规则：`W + 用户account`）
- `user_id`: 用户 ID，唯一（1 个用户仅 1 个钱包）
- `balance`: 余额，`decimal(19,2)`
- `currency`: 币种，默认 `CNY`
- `status`: 钱包状态，默认 `ACTIVE`
- `created_at`: 创建时间
- `updated_at`: 更新时间

约束说明：
- `wallet_no` 唯一
- `user_id` 唯一

钱包流水表 `wallet_flow` 字段说明：
- `id`: 主键，自增
- `wallet_id`: 钱包主键 ID
- `wallet_no`: 钱包号
- `user_id`: 用户 ID
- `business_no`: 业务单号
- `change_type`: 流水类型（`RECHARGE`/`CONSUME`）
- `amount`: 变更金额
- `before_balance`: 变更前余额
- `after_balance`: 变更后余额
- `remark`: 备注
- `created_at`: 流水创建时间

### 2.2 业务流程
1. 用户注册成功后，自动调用 `createWalletIfAbsent` 创建钱包。
2. 查询钱包时，若历史用户尚未开户，会进行懒创建。
3. 充值与消费通过数据库悲观锁读取钱包记录，避免并发下余额覆盖。
4. 充值/消费完成后，写入 `wallet_flow` 钱包流水。
5. 消费前校验余额充足，不足则返回业务错误。

### 2.3 金额规则
- `amount` 必填。
- `amount > 0`。
- 当前金额使用两位小数精度。

### 2.4 代码落点
- 实体：`src/main/java/com/lujianfeng/spanner/entity/user/WalletAccountEntity.java`
- 仓库：`src/main/java/com/lujianfeng/spanner/repository/WalletAccountRepository.java`
- DTO：`src/main/java/com/lujianfeng/spanner/dto/user/WalletAmountChangeRequestDTO.java`
- VO：
  - `src/main/java/com/lujianfeng/spanner/vo/user/WalletAccountVO.java`
  - `src/main/java/com/lujianfeng/spanner/vo/user/WalletChangeResultVO.java`
  - `src/main/java/com/lujianfeng/spanner/vo/user/WalletFlowItemVO.java`
- 服务接口：`src/main/java/com/lujianfeng/spanner/service/service/UserService.java`
- 服务实现：`src/main/java/com/lujianfeng/spanner/service/impl/UserServiceImpl.java`
- 控制器：`src/main/java/com/lujianfeng/spanner/controller/UserController.java`
- 流水仓库：`src/main/java/com/lujianfeng/spanner/repository/WalletFlowRepository.java`

## 3. 安全与事务
- 接口路径均在 `/user/**` 下，默认需要 JWT 鉴权（沿用现有安全配置）。
- 充值/消费方法添加 `@Transactional`。
- 充值/消费读取钱包时使用 `PESSIMISTIC_WRITE` 锁，降低并发写冲突风险。
- 流水查询支持分页参数 `page/size` 与类型过滤 `changeType`。

## 4. 后续可扩展建议
- 增加 `businessNo + changeType + userId` 幂等唯一约束。
- 增加冻结余额与可用余额拆分。
- 增加钱包状态流转（ACTIVE/FROZEN/CLOSED）。
- 增加按时间范围、业务单号等多维流水检索能力。
