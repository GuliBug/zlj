# 提交上架生效

## Why

管理端「新增商品」支持两种提交方式：「保存草稿」（status=OFF）与「提交上架」（status=ON，提示「已提交上架，H5 端即时可见」）。但后端 `ProductService.create()` 硬编码 `product.setStatus("OFF")`，忽略了请求体中的 `status` 字段，导致「提交上架」实际产出已下架商品：商家看到上架成功提示，买家端却不可见。该缺陷由 UI 级端到端用例 WC-20（新增商品三步真实提交上架）的截图证据暴露（列表状态列显示「已下架」），接口级用例未覆盖「create 直接携带 status=ON」路径故此前未拦截。

## What Changes

### 后端

- `ProductService.create()`：按请求体 `status` 字段落库，仅接受 `ON`/`OFF` 字面值，非法或缺省回退 `OFF`（草稿安全兜底）；其余逻辑（违禁词校验、SKU 保存、审计日志）不变。

### 前端

#### Web 管理端（frontend-web，仅测试脚本）

- `tests/e2e/ui-e2e-web.mjs` WC-20 断言加强：提交上架成功后除「商品存在」外，必须断言接口返回的 `status === 'ON'`，防止回归。

#### H5（frontend-h5）

- 无代码改动（买家端只读商品列表，状态由后端保证）。

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `商品/backend`：FR-10-1 商品新增补充「提交上架即上架」场景（status 字段语义生效）。

## Impact

### 后端

- 影响：`mall/service/ProductService.java`（create 方法一行级改动）；接口 `POST /admin-api/products` 请求字段 `status` 从「被忽略」修正为「生效」，无字段结构变化。

### 前端

- 影响：`tests/e2e/ui-e2e-web.mjs`（测试断言加强）；产品代码无改动。

## 非目标

- 不改商品编辑（update）的状态语义（编辑不改状态，上下架仍走 `POST /admin-api/products/status`）。
- 不调整前端提交上架交互（提示文案、确认弹窗保持现状）。
- 不补「保存草稿 → 再次编辑上架」的跨用例链路（现有用例已覆盖独立上下架）。

## 修订记录

| 日期 | 摘要 | 提出方 |
|---|---|---|
| 2026-10-24 | 初稿定稿（BUG-09 修复） | 用户 |
