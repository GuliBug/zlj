# Design: fix-order-detail-receiver-info

## 现状与根因

后端 `OrderService.orderDetailVo`（`backend/.../service/OrderService.java:626`）返回扁平字段：

```json
{ "receiver": "张三", "receiverPhone": "138****", "address": "湖南省长沙市……" }
```

前端 `frontend-h5/src/pages/OrderDetail.vue:125-128`：

```js
const recv = computed(() => {
  const x = d.value
  return x.address || (x.receiver ? { receiver: x.receiver, phone: x.receiverPhone, addressText: x.address } : null)
})
```

`x.address` 是真值字符串 → `recv` 恒为字符串 → 模板第 61-62 行对字符串做属性访问，`recv.region` 等为 `undefined`，兜底表达式 `recv.region + ' ' + (recv.detail || '')` 无空值防护 → 页面渲染 `undefined`。

## 契约疑问

按协作约定在此记录（不擅自改主规格，随本 change 的 delta 归档确认）：

- `api-contract` AC-06 对 `GET /api/orders/{no}` 只写了「含收货信息」，未定义字段结构。现有后端实现为扁平字段 `receiver/receiverPhone/address`，与地址簿接口 AC-05 的 `{receiver, phone, region, detail, isDefault}` 结构不同。
- 本 change 取**扁平字段为契约事实**（后端不改），以 MODIFIED delta 补充进 AC-06；管理端同源数据结构不受影响。

## 方案

仅改 `frontend-h5/src/pages/OrderDetail.vue`：

1. `recv` computed 修正（唯一逻辑改动）：

```js
const recv = computed(() => {
  const x = d.value
  if (!x) return null
  return { receiver: x.receiver || '', phone: x.receiverPhone || '', addressText: x.address || '' }
})
```

2. 模板第 58-65 行收货信息卡片简化：第一行 `{{ recv.receiver }} {{ recv.phone }}`、第二行 `{{ recv.addressText }}`；两侧内容均为空时整卡显示「—」（保留现有 `v-if` 语义：对象存在即渲染，空串字段不输出 `undefined`）。

3. 兼容性收口：`pickupStore` computed 与本问题无关，不动；`fulfillmentType === 'PICKUP'` 分支不动。

## 备选与取舍

- **备选 A**：后端改为返回嵌套 `address` 对象对齐地址簿结构。拒绝：破坏管理端与既有 E2E（`tests/e2e/e2e-mall.mjs`）依赖的返回结构，改动面大且无收益。
- **备选 B**：仅修模板兜底链（保留 `recv` 现状，在模板上加 `typeof` 判断）。拒绝：把「字符串当对象」的错误语义留在代码里，后续维护成本高。

## 测试要点

- 手工：下单 EXPRESS 订单 → 订单详情收货信息显示「收货人 手机号」+ 完整地址，无 `undefined`。
- 手工：PICKUP 订单详情仍显示自提门店卡片（回归）。
- E2E：`node tests/e2e/e2e-mall.mjs` 全量 PASS（覆盖订单详情断言）。
