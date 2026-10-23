# Proposal: add-sales-accumulation

## Why

用户实测：完成下单（订单已到达「已完成」状态）后，商品详情/列表中的「已售」（`sales` 字段）始终为 0。排查确认：

- **前端无问题**：H5 与 Web 端均原样渲染接口返回的 `sales`（契约 AC-03 已定义该字段）。
- **后端缺陷**：`sales` 字段（`Product` 与 `ProductSku`）仅在商品创建时初始化为 0（`ProductService` L196/L256/L405 `setSales(0)`），整个订单生命周期——支付实扣（`OrderService` L244-247）、自提核销实扣（L490-492）、退款回补——**没有任何位置对 `sales` 做累加**，导致「已售」永远为 0。
- **规格空白**：主规格 `api-contract` AC-03 仅定义 `sales` 字段返回，`catalog` 主规格无任何 Requirement 描述「已售随成交累计」的可观测行为。

## What Changes

- 仅改 `backend`：
  1. `ProductSkuRepository` / `ProductRepository` 新增原子累加 SQL（`sales = COALESCE(sales, 0) + :qty`）；
  2. 在库存实扣唯一入口 `StockService.deduct()` 的事务内同步累加 SKU 级与商品级 `sales`（EXPRESS 支付实扣与 PICKUP 核销实扣均经过该入口，无需在 `OrderService` 重复埋点）；
  3. E2E 新增 1 条断言：下单支付后商品详情 `sales` 相应增加。
- 不改接口契约（`sales` 字段含义不变），不改前端。

## Impact

- 影响 spec：`catalog`（ADDED `FR-02-6 已售数量累计`）。
- 影响代码：`backend/.../repository/ProductSkuRepository.java`、`ProductRepository.java`、`service/StockService.java`、`tests/e2e/e2e-mall.mjs`。
- 退款不递减已售（决策见 design.md），不涉及 H5/Web 前端。
