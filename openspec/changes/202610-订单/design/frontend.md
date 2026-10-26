## Context

H5 侧：Checkout.vue（快递/自提分段、地址选用、积分开关、预估金额、提交后金额明细态）、Orders.vue（6 状态 Tab + 行内操作）、OrderDetail.vue（时间轴/核销码/金额明细/吸底操作）、PayResult.vue（结果引导）；Web 管理端 views/order/index.vue（筛选/发货/批量发货/导出）。

## Goals / Non-Goals

**Goals:**

- 记录订单域前端基线交互口径（防重、确认弹窗、服务端金额优先、操作按状态渲染）。

**Non-Goals:**

- 不改动任何代码；不新增支付方式。

## Decisions

### FD1 提交防重与幂等键

结算页进入时生成 `uuid()` 幂等键，提交按钮 loading 防重；成功后立即换新键并切换「订单已提交」金额明细态（全部展示服务端返回值）。

### FD2 金额展示口径

预估金额标注「最终金额以下单后服务端计算为准」；详情/支付结果一律渲染服务端字段（goodsAmount/freight/pointsDeduction/payAmount）。

### FD3 危险操作二次确认

取消订单、确认收货均 `showConfirm` 二次确认（文案说明后果：关闭并释放库存/确认后订单完成）。

### FD4 行内操作按状态渲染

待支付：取消订单/去支付；待自提：出示核销码（跳核销码页携 orderId）；已发货：确认收货；已完成/售后中：申请售后；售后进行中详情页吸底按钮切换「查看售后进度」。

### FD5 buyNow 与购物车双结算入口

立即购买经 `store.buyNow`（用后即清）；购物车结算取勾选有效行；两入口共用结算页与幂等提交逻辑。

### FD6 契约疑问章节

无疑问：订单/核销码字段与契约一致。

### FD7 收货信息字段形态兼容（BUG-11 修复，2026-10-26 补录）

- 现象：快递订单详情页「收货信息」卡渲染出字面量 `undefined`。
- 根因：后端详情接口收货信息为平铺字段——`receiver` / `receiverPhone` / `address`（收货地址**文本**，下单时由 region + detail 拼好落库）；而 `OrderDetail.vue` 的 `recv` computed 把非空字符串 `x.address` 直接当对象返回，模板对字符串取 `.region` 得 `undefined` 拼出「undefined」。
- 结论：`recv` computed 先判 `typeof x.address === 'object'`（对象形态兼容预留），字符串形态时构造 `{ receiver, phone, addressText }` 再交给模板；姓名/电话/地址三行渲染分支不变。
- 契约疑问：无——契约口径即平铺字段，前端此前误用对象形态。

## Risks / Trade-offs

- [支付结果页补一击支付] → 携 pay=1 进入时先调一次 pay（幂等，已支付忽略），兜住「列表去支付」直达场景。

## Open Questions

（无）

## 修订记录

| 日期 | 摘要 | 提出方 |
|---|---|---|
| 2026-10-26 | 补录 FD7：收货信息 undefined（BUG-11）修复口径，纯前端字段形态兼容 | 用户（实测发现） |
