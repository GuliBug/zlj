## Context

订单域实现于 `OrderController/OrderService`（下单/列表/详情/支付/取消/确认/核销码）、`OrderTimeoutTask`（超时关单）与 `AdminOrderController`（列表/发货/批量发货/导出）。状态机单向流转，库存预占与订单同一事务。

## Goals / Non-Goals

**Goals:**

- 记录订单域基线设计口径（计价、幂等、预占、核销码、状态机）。

**Non-Goals:**

- 不改动任何代码；不接入真实支付网关（模拟通道保留接入点）。

## Decisions

### BD1 服务端计价唯一权威

商品合计/运费/积分抵扣/应付全部由服务端以「分」整数计算；前端金额仅预估展示并标注「以服务端计算为准」，杜绝端上算钱分歧。

### BD2 幂等键 5 分钟窗口

客户端生成 UUID 随单提交，`IdempotentRecord` 5 分钟内去重返回首次订单号；提交成功后前端立即换新键。

### BD3 库存预占乐观锁

下单与预占同一事务，条件更新失败返回缺货 SKU 与剩余可购数量并整单回滚；取消/超时（15 分钟）释放预占生成 RELEASE 流水。

### BD4 核销码分钟窗口哈希

6 位码按分钟窗口服务端计算，核销接受当前 + 上一窗口码（容忍跨分钟出示）；买家端倒计时 `expiresInSec` 自动刷新，防截图盗用。

### BD5 支付模拟通道幂等

POST pay 仅 WAIT_PAY 可成功，重复调用拒绝并登记 DUPLICATE_PAY 告警；接口语义保留真实支付回调接入点。

### BD6 状态机集中校验

状态流转集中在 OrderService 校验，非法流转抛异常（含对 CLOSED 再支付、未发货确认收货等回跳尝试），时间轴逐节点追加。

### BD7 订单项快照图列为大文本（BUG-12 修复，2026-10-26 补录）

- 现象：含 base64 主图商品（如 UI E2E WC-20 新增的商品）下单报「系统繁忙」，后端 `order_item` insert 抛 `Value too long for column "IMAGE CHARACTER VARYING(255)"`（快照 dataURL 约 21KB）。
- 根因：真实图片上传（change：202610-真实图片上传）后 `Product.images` 存 dataURL，订单项快照图取 `firstImage(product.getImages())` 也是完整 dataURL；但 `OrderItem.image` 未同步加 `@Lob`，H2 建表仍为 VARCHAR(255)。4 个落库图片列（Product.images/detailImages、AfterSale.images、OrderItem.image）中唯独它漏掉。
- 结论：`OrderItem.image` 加 `@Lob`（H2=CLOB，MySQL=LONGTEXT）；H2 内存库重启即重建 schema，无历史数据迁移。
- 契约疑问：无——接口字段结构不变。

## Risks / Trade-offs

- [模拟支付即成功] → 演示环境可接受；生产接入必须替换为回调 + 主动查询双通道（契约已留位）。

## Open Questions

（无）

## 修订记录

| 日期 | 摘要 | 提出方 |
|---|---|---|
| 2026-10-26 | 补录 BD7：OrderItem.image 加 @Lob（BUG-12，base64 快照超 VARCHAR(255) 致下单 500） | 用户（实测下单失败） |
