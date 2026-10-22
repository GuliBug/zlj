# Design: 2026-10-22-align-prototype-and-fix-order-detail

## 1. 订单详情 items 缺失

`orderDetailVo(order)` 基于 `orderVo(order)` 组装，但 `items` 仅在列表接口（`page`/`adminPage`）单独 put。修复：详情同样 `data.put("items", itemVos(...))`；`itemVos` 补 `skuId`、`subtotal`（前端购物车跳转售后等场景需要）。

## 2. 原型对齐策略

- **原型优先、契约兜底**：页面结构/文案/视觉以两个原型 HTML 为单一基准；数据一律走真实接口，原型中的假数据数组仅作文案与布局参考。
- **H5**：hash 路由 + pages.json 单一来源保留；TabBar 4 项（首页/全部商品/购物车/我的），订单/地址/售后/门店/核销码从「我的」进入（对齐原型导航模型）；保留 UI 自动化依赖类名（.bottom-bar/.btn/.sheet/.sku-ok），Checkout 履约切换 .f-tab → .seg 并同步 ui-e2e.mjs。
- **图片防破图**：新增 GoodsMedia 组件——image 有效用 <img>（onerror 隐藏回退），否则/失败回退渐变红底+楷体首字品牌块。
- **Web**：Element Plus 保留为交互底座，主题变量覆盖为原型色（--primary #8f1d22、侧栏 #241d18、bg #f4f1ea）；审计与告警从侧栏移至工作台资损监控入口（原型「合规中心已移除」的决策）。

## 3. 种子数据

- Store 实体新增 `operatingHours`（ddl-auto=update 自动建列），种子写入营业时间；列表直接返回实体自动携带。
- 分类由两级（系列）改为四档（口粮/进阶/待客/礼盒典藏），与原型 chips 一致；商品 6 个全部以原型命名与定价（30/66/99/168/500/1280 元）。
- 等级名对齐原型：V1 银卡掌柜 / V2 金卡掌柜 / V3 钻卡掌柜（阈值 500/2000/5000 元不变）。
- 搜索 keyword 增加 brand 匹配，保证 E2E 品牌搜索用例与新种子兼容。

## 4. 兼容性

- 接口只增不改（items/operatingHours 为新增字段），契约 delta 以 ADDED 呈现。
- E2E（e2e-mall.mjs）无需改动即通过；ui-e2e.mjs 选择器同步一处。
