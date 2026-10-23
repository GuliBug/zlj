## Context

frontend-web（Vue3 + Element Plus + Pinia + vue-router）：侧栏 7 菜单（工作台/商品/库存/门店/订单/售后/会员），审计与告警自工作台资损监控入口进入；主色 #8f1d22、侧栏 #241d18（对齐 Web 管理端原型）。

## Goals / Non-Goals

**Goals:**

- 记录管理端前端基线结构口径（原型对齐、危险操作确认、视觉主题）。

**Non-Goals:**

- 不改动任何代码；不新增菜单。

## Decisions

### FD1 结构对齐原型文件

以 `D:/projects/zlj/需求/槟榔商城-Web管理端原型.html` 为结构基准：菜单划分、指标卡布局、新增/编辑商品独立页面（三步步骤条）均与原型一致，页面数据来自真实接口而非原型示例数据。

### FD2 危险操作统一二次确认

上下架、退款、积分调整（双重确认）、盘点确认、删除地址等操作一律 `ElMessageBox.confirm`；退款/积分调整等资金类为错误级「最终确认」。

### FD3 登录态与路由守卫

token 存 Pinia（utils/auth.js 持久化），路由守卫校验未登录跳登录页（携 redirect）；401 响应统一拦截清 token 跳登录。

### FD4 请求层统一

utils/request.js 封装 axios：统一 X-Token 注入、`{code,data,msg}` 解包、错误 ElMessage 提示；金额展示统一走 utils/format.js 的 `fen2yuan`。

### FD5 契约疑问章节

无疑问：各页面消费接口与契约一致。

## Risks / Trade-offs

- [原型为静态 HTML，交互细节以实现为准] → 结构/主题/菜单强对齐，微交互（loading/空态）按 Element Plus 惯例实现。

## Open Questions

（无）
