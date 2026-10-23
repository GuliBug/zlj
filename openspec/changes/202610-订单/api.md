## 通用约定

- 响应包装 `{ code, data, msg }`，code=200 成功、401 未登录；分页 `page`/`size`，数据 `{total, rows}`；金额一律整数「分」；订单接口整体需会员登录。
- 本 change 为「订单」能力基线，无接口变更；以下为当前实现的前后端联调接口快照（与「接口契约」主规格 AC-06/AC-07/AC-13 一致）。

## 接口总览

### 买家端（H5 消费）

| HTTP | URL | 说明 | 契约 |
|---|---|---|---|
| POST | /api/orders | body `{idempotentKey, fulfillmentType(EXPRESS\|PICKUP), storeId?/addressId?, usePoints, items:[{skuId, quantity}]}` → `{orderNo, goodsAmount, freight, pointsDeduction, payAmount, pointsUsed, status}`；服务端计价、幂等 5 分钟、缺货 msg 含 SKU 与剩余可购量 | AC-06 |
| GET | /api/orders?status=&page=&size= | 状态筛选分页，行含 items 摘要与 statusText | AC-06 |
| GET | /api/orders/{no} | 详情：金额明细/收货信息或自提门店/物流/时间轴（合并售后节点「售后 · 」前缀）/PICKUP_WAIT 含 pickupCode/含 aftersale 摘要 | AC-06 |
| POST | /api/orders/{no}/pay | 模拟支付；非 WAIT_PAY 幂等拒绝 | AC-06 |
| POST | /api/orders/{no}/cancel | 仅 WAIT_PAY 可取消，释放预占 | AC-06 |
| POST | /api/orders/{no}/confirm | 仅 SHIPPED 可确认收货 → COMPLETED | AC-06 |
| GET | /api/orders/{no}/verify-code | → `{code(6位), expiresInSec(≤60)}` 分钟窗口动态核销码 | AC-07 |

### 管理端（frontend-web 订单页消费）

| HTTP | URL | 说明 | 契约 |
|---|---|---|---|
| GET | /admin-api/orders?status=&keyword=&page=&size=&overdueShip= | keyword 匹配订单号/脱敏手机号；超 24h 未发货行 `shipOverdue:true` | AC-13 |
| POST | /admin-api/orders/{no}/ship | body `{company, no}`，仅 PAID 可发 | AC-13 |
| POST | /admin-api/orders/batch-ship | CSV 批量发货 → `{success, failures:[{orderNo, reason}]}` | AC-13 |
| GET | /admin-api/orders/export?from=&to= | 订单 CSV 导出（含金额明细列） | AC-13 |

## 说明

- 状态机：WAIT_PAY→PAID→SHIPPED→COMPLETED；自提 PAID→PICKUP_WAIT→COMPLETED；WAIT_PAY→CLOSED；COMPLETED↔AFTER_SALE，非法流转拒绝。
- 运费：EXPRESS 满 9900 分包邮否则 1000 分；PICKUP 恒 0；积分 100=1 元且 ≤5%；单笔 ≤500000 分。
- 变更记录：2026-10-23 基线快照，无接口变更；影响下游：H5 结算/订单列表/详情/支付结果、Web 订单管理页。
