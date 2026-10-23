# Design: add-sales-accumulation

## 根因与修复思路

`sales` 全链路只写不累加：`ProductService` 创建商品/SKU 时 `setSales(0)`（L196/L256/L405）后，订单链路（支付实扣 `OrderService` L244-247、自提核销实扣 L490-492）均未触碰该字段。前端只渲染接口值，无问题。

## 关键决策

### D1：累加时机 = 库存实扣（而非下单预占）

- 预占阶段（下单）可能被取消/超时释放，若在预占时累加，取消后已售虚高需回退，引入额外复杂度。
- 实扣是成交事实发生点（EXPRESS 支付成功、PICKUP 核销完成），且是**唯一入口**：`StockService.deduct()`（`StockService` L58-64）。在此一处累加，`OrderService` 两处调用点自动生效，无需重复埋点。
- 被拒方案：在 `OrderService.pay()`/`verify()` 分别累加 — 两处埋点，遗漏风险高。

### D2：退款不递减已售

- PRD 与契约（AC-03）均未要求退款回退已售；主流电商「已售」为累计成交口径。
- 递减会让已售随售后波动，且 `refundIn`（退款回补）与「销售」语义相反，保持已售只增不减最简单且可解释。
- 契约无变更，故本 change 不动 `api-contract`。

### D3：商品级与 SKU 级同时原子累加

- `Product.sales`（列表/详情展示）与 `ProductSku.sales`（SKU 维度展示）都对外暴露，需同时累加。
- 实现为 JPQL 原子更新 `UPDATE ... SET sales = COALESCE(sales, 0) + :qty`，与既有 `deductReserved`/`release`/`refundIn` 风格一致，避免读-改-写竞态。
- `StockService.deduct` 事务内先按 `skuId` 取 `productId`（`skuRepository.findById`），再原子更新两表。

## 契约疑问

无。`sales` 字段已在 AC-03 定义，本次仅修复其取值语义（恒 0 → 随成交累计）。

## 验证方式

- 后端 `mvnw compile` 零报错。
- E2E 新增 1 条断言：创建测试商品 → 买家下单支付 → 查商品详情 `sales` 增加对应数量 → 取消一笔待支付订单 → `sales` 不变。
- 全量回归 `node tests/e2e/e2e-mall.mjs` 125+1 全通过。
