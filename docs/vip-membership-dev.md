# 用户 VIP 会员开发文档

## 1. 功能目标
在现有用户与钱包体系上新增会员能力，支持：
- 月费会员（20 元/月）
- 季度会员、年费会员
- 会员到期时间管理（开通/续费叠加）
- 成长值累积与等级计算（类似 QQ 等级）
- 会员订单留痕与查询

## 2. 方案概览
- 复用钱包余额与安全密码能力完成扣费。
- 新增 `user_vip_order` 记录会员开通订单。
- 在 `user_info_test` 增加用户成长与会员字段：
  - `vip_expire_at`
  - `growth_value`
  - `user_level`
- 通过成长值阈值计算等级，购买会员时自动增加成长值。
- 会员开通成功后，通过事务后事件推送系统私信通知（在线实时、离线兜底）。

## 3. 套餐与成长值规则
当前固定套餐定义在 `VipPlanType`：
- `MONTHLY`：20.00 元，1 个月，成长值 +100
- `QUARTERLY`：58.00 元，3 个月，成长值 +350
- `YEARLY`：218.00 元，12 个月，成长值 +1500

等级阈值（`LEVEL_THRESHOLDS`）：
- L1: 0
- L2: 100
- L3: 300
- L4: 700
- L5: 1500
- L6: 3000
- L7: 6000
- L8: 12000
- L9: 25000
- L10: 50000

说明：
- `growthValue >= 阈值` 即达到对应等级。
- `nextLevelGrowth` 返回下一级阈值；满级后返回 `null`。

## 4. 数据模型

### 4.1 用户表扩展（`user_info_test`）
- `vip_expire_at timestamp`
- `growth_value bigint`
- `user_level int`

### 4.2 会员订单表（`user_vip_order`）
字段：
- `id`：主键
- `user_id`：用户 ID
- `purchase_no`：购买单号（唯一）
- `plan_code`：套餐编码
- `plan_name`：套餐名称
- `amount`：支付金额
- `months`：开通月数
- `growth_bonus`：赠送成长值
- `start_at`：本次生效起始时间
- `end_at`：本次生效截止时间
- `status`：当前状态（当前使用 `SUCCESS`）
- `created_at`、`updated_at`：时间戳

索引：
- `idx_user_vip_order_user_created(user_id, created_at)`
- `idx_user_vip_order_purchase_no(purchase_no)` 唯一

### 4.3 钱包流水扩展
- 新增流水类型：`VIP_PURCHASE`。

## 5. 核心流程

### 5.1 购买会员（`purchaseVip`）
1. 校验登录用户与请求参数。
2. 解析套餐（默认 `MONTHLY`）。
3. 锁定钱包并校验安全密码。
4. 校验余额后扣款，写入钱包流水 `VIP_PURCHASE`。
5. 计算会员时间：
   - 若当前 `vipExpireAt > now`，从当前到期时间继续叠加；
   - 否则从 `now` 开始计算。
6. 增加成长值并重算等级。
7. 写入 `user_vip_order` 订单。
8. 发布 `VipOpenedNotifyDomainEvent` 事件，事务提交后发送系统通知。

### 5.2 增加成长值（`addMyGrowth`）
1. 校验 `growthValue > 0`。
2. 叠加成长值并重算等级。
3. 返回最新会员资料。

### 5.3 查询会员资料（`getMyVipProfile`）
- 返回是否有效会员（`vipExpireAt > now`）、到期时间、成长值、等级、下级阈值。

## 6. 代码落点
- 套餐枚举：`/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/src/main/java/com/lujianfeng/spanner/entity/user/VipPlanType.java`
- 会员订单实体：`/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/src/main/java/com/lujianfeng/spanner/entity/user/UserVipOrderEntity.java`
- 会员订单仓库：`/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/src/main/java/com/lujianfeng/spanner/repository/UserVipOrderRepository.java`
- 服务接口：`/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/src/main/java/com/lujianfeng/spanner/service/service/UserService.java`
- 服务实现：`/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/src/main/java/com/lujianfeng/spanner/service/impl/UserServiceImpl.java`
- 会员通知事件：`/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/src/main/java/com/lujianfeng/spanner/event/message/VipOpenedNotifyDomainEvent.java`
- 会员通知监听器：`/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/src/main/java/com/lujianfeng/spanner/event/message/VipOpenedNotifyListener.java`
- 控制器：`/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/src/main/java/com/lujianfeng/spanner/controller/UserController.java`
- DTO：
  - `/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/src/main/java/com/lujianfeng/spanner/dto/user/VipPurchaseRequestDTO.java`
  - `/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/src/main/java/com/lujianfeng/spanner/dto/user/UserGrowthChangeRequestDTO.java`
- VO：
  - `/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/src/main/java/com/lujianfeng/spanner/vo/user/VipPlanVO.java`
  - `/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/src/main/java/com/lujianfeng/spanner/vo/user/VipProfileVO.java`
  - `/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/src/main/java/com/lujianfeng/spanner/vo/user/VipPurchaseResultVO.java`
  - `/Users/luzhouyue/Documents/GitHub/Spanner_be_spring/src/main/java/com/lujianfeng/spanner/vo/user/VipOrderItemVO.java`

## 7. 事务与一致性
- `purchaseVip`、`addMyGrowth` 使用事务，避免中间态。
- 购买流程中钱包读取使用悲观锁，保证余额扣减并发安全。
- `purchaseNo` 做唯一校验，避免重复订单号写入。
- 系统通知使用 `@TransactionalEventListener(phase = AFTER_COMMIT)`，确保仅在购买事务成功提交后发送，避免回滚脏通知。

## 8. 配置与迁移说明
- 当前项目 `spring.jpa.hibernate.ddl-auto=update`，启动后会自动：
  - 给用户表补充新字段；
  - 创建 `user_vip_order` 表及索引。
- 生产环境建议切换为显式迁移（Flyway/Liquibase）管理 DDL。

## 9. 后续扩展建议
- 增加会员自动续费与到期提醒任务。
- 增加成长值变更流水表（目前仅汇总值）。
- 把等级阈值和套餐改为可配置（数据库或配置中心）。
- 增加会员权益拦截器（如上传上限、功能权限、标识展示）。
