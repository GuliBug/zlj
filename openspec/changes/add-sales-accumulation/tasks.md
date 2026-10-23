# Tasks: add-sales-accumulation

## 1. 实现

- [x] 1.1 `ProductSkuRepository` 新增 `increaseSales(skuId, qty)` 原子累加方法
- [x] 1.2 `ProductRepository` 新增 `increaseSales(productId, qty)` 原子累加方法
- [x] 1.3 `StockService.deduct()` 事务内累加 SKU 级与商品级 `sales`（含 COALESCE 兜底历史 0/null）

## 2. 验证与门禁

- [x] 2.1 `tests/e2e/e2e-mall.mjs` 新增断言：支付实扣后 `sales` 增加、取消待支付订单不影响 `sales`（TC-FR-02-06 × 2）
- [x] 2.2 后端 `mvnw compile` 零报错
- [x] 2.3 全量 E2E 回归通过并更新 `docs/测试用例.md` / `docs/测试报告.md`（127/127，100%）
