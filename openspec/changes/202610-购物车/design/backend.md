## Context

购物车实现于 `CartController/CartService`（AC-04 六个接口），购物车行含实时库存与失效标识，`totalQuantity` 作为 H5 TabBar 角标数据源。

## Goals / Non-Goals

**Goals:**

- 记录购物车基线设计口径（实时性、上限、角标数据源）。

**Non-Goals:**

- 不改动任何代码；不做购物车持久化迁移（跟随会员存储）。

## Decisions

### BD1 实时性在查询时计算

`stock` 与 `invalid` 不冗余存储，查询购物车时按 SKU 当前状态实时计算——商品下架/库存变化立即反映，避免后台任务同步的复杂度。

### BD2 双层上限防爆

单 SKU 上限 999、SKU 种类上限 100（报「购物车已满」），防止异常灌单拖垮列表渲染与结算。

### BD3 角标取 totalQuantity

H5 角标不自行累加，统一以 GET /api/cart 的 totalQuantity 为唯一数据源，加购/删除/改数后调用 `refreshCartCount()` 刷新，保证多页面角标一致。

## Risks / Trade-offs

- [查询时逐行计算库存] → 购物车种类上限 100，计算量可控；如上限放宽需评估批量查询。

## Open Questions

（无）
