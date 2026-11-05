## Context

H5 购物车页 Cart.vue：失效商品分区（一键清除）、有效行（勾选/步进/删除）、吸底合计栏（类名 `.bottom-bar`/`.btn` 为 UI 自动化依赖）、空态引导；角标由 store.js 的 `refreshCartCount()` 全局维护。

## Goals / Non-Goals

**Goals:**

- 记录购物车前端基线交互口径（步进边界、删除确认、结算流转）。

**Non-Goals:**

- 不改动任何代码；不改吸底栏类名（UI 自动化依赖）。

## Decisions

### FD1 步进边界贴实时库存

「＋」在该行 `quantity >= stock` 时禁用；减至 1 再减触发删除二次确认（等价于移出购物车）；合计只统计勾选的有效行。

### FD2 删除与清除均需确认

单行删除弹确认框（含商品名）；「一键清除」直接调用 clear-invalid 并 Toast 反馈（列表仅剩失效行时的批量操作，不逐行确认）。

### FD3 结算流转

未勾选任何有效行点击「去结算」提示拦截；结算页仅取 `checked && !invalid` 行——缺货行即使勾选也不进入结算，规避提交必然失败。

### FD4 契约疑问章节

无疑问：rows/totalQuantity 字段与契约一致。

## Risks / Trade-offs

- [库存不足行保留可勾选] → 依赖结算页过滤兜底；如需更严格可在勾选时即时拦截（当前保持操作轻量）。

## Open Questions

（无）
