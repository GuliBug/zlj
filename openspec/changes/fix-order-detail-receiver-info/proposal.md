# Proposal: fix-order-detail-receiver-info

## Why

H5 订单详情页「收货信息」卡片显示字面量 `undefined`（截图见用户反馈）。买家无法核对收货人/电话/地址，属于订单详情核心信息展示缺陷，违反 `h5-client` H5-11 的场景「买家核对订单商品：收货信息或自提门店完整显示」。

## 根因

- 后端 `GET /api/orders/{no}` 返回的是**扁平字段**：`receiver`（收货人）、`receiverPhone`、`address`（完整地址字符串）。
- 前端 `OrderDetail.vue` 的 `recv` computed 把 `x.address`（字符串）误当作收货对象优先返回：
  `x.address || (x.receiver ? {...} : null)`。
- 由于 `address` 是真值字符串，`recv` 恒为字符串，模板中 `recv.fullAddress` 等属性访问全为 `undefined`，最终兜底表达式 `recv.region + ' ' + (recv.detail || '')` 缺空值防护，渲染出 `undefined`。

## What Changes

- **仅改前端**（`frontend-h5/src/pages/OrderDetail.vue`）：修正 `recv` computed 的取值逻辑——`x.address` 为字符串时构造 `{receiver, phone: x.receiverPhone, addressText: x.address}` 对象；模板兜底链补齐空值防护，任何字段缺失时显示「—」而非 `undefined`。
- **契约澄清**（delta）：`api-contract` AC-06 订单详情收货信息字段结构此前未明确，本 change 以 MODIFIED delta 明确为扁平字段 `{receiver, receiverPhone, address}`，归档时同步主规格。
- 后端不改代码（现有返回结构即契约意图）。

## Impact

- 影响 spec：`h5-client`（新增收货信息渲染场景）、`api-contract`（AC-06 字段结构澄清）。
- 影响代码：`frontend-h5/src/pages/OrderDetail.vue`（约 10 行内改动）。
- 无数据库/接口破坏性变更，无其他页面受影响（自提门店分支不涉及）。
