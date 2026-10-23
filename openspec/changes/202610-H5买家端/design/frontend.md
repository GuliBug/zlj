## Context

frontend-h5：pages.json 为页面配置唯一来源（window 主题/pages 清单/tabBar 4 项），App.vue 全局样式，api.js 封装请求与 401 拦截，store.js 维护购物车角标与全局门店/结算状态，ui.js 提供 Toast/Confirm/Alert，GoodsMedia/BrandNav/TabBar 为公共组件。

## Goals / Non-Goals

**Goals:**

- 记录 H5 前端基线架构口径（配置单一来源、全局拦截、视觉规范）。

**Non-Goals:**

- 不改动任何代码；不恢复门店切换（单店模式）；不引入 Taro。

## Decisions

### FD1 pages.json 单一来源

路由、标题、登录拦截与 TabBar 渲染均由 pages.json 生成，组件内禁止硬编码页面清单；新增页面只改配置 + 落文件。

### FD2 401 全局拦截

api.js 统一解包 `{code,data,msg}`；401 清 token 并跳登录页（携 redirect 回跳）；页面级 catch 只做兜底渲染不重复跳转。

### FD3 破图兜底组件化

GoodsMedia 统一处理图片空/404 → 渐变红底品牌占位（楷体首字），列表/详情/购物车/订单全站复用。

### FD4 吸底栏与 TabBar 叠放

`.bottom-bar` 在含 TabBar 页面抬升至 `bottom: 52px`，页面留白预留两层高度；关键按钮经 elementFromPoint 校验不被遮挡（UI E2E 断言依赖）。

### FD5 单店模式全局状态

store.js `applyDefaultStore` 固定默认门店；`selectedStoreId/checkoutAddressId/buyNow` 支撑结算流转，buyNow 用后即清。

### FD6 视觉规范

红金国潮老字号风（品牌印记「正隆記·始于一九一六」、宗旨「以正立本，以究待客」），375px 基准适配；健康警示条与「不向未成年人销售」固定展示。

### FD7 契约疑问章节

无疑问：页面消费接口与契约一致（映射见 api.md）。

## Risks / Trade-offs

- [无骨架屏] → 页面加载态以空态/兜底呈现，演示场景可接受；弱网优化列为后续增量。

## Open Questions

（无）
