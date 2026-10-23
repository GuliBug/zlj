## ADDED Requirements

### Requirement: H5-12 订单详情收货信息渲染
订单详情页（EXPRESS 订单）的「收货信息」卡片 SHALL 基于后端扁平字段 `receiver` / `receiverPhone` / `address`（完整地址字符串）渲染收货人与地址；任一所需数据整体缺失时 SHALL 显示占位符「—」，任何情况下不得渲染出字面量 `undefined` / `null`。

#### Scenario: 收货信息完整展示
- **WHEN** 买家查看 EXPRESS 订单详情且后端返回 `receiver/receiverPhone/address`
- **THEN** 卡片显示「收货人 手机号」与完整地址，无 `undefined` 字样

#### Scenario: 收货信息数据缺失兜底
- **WHEN** 订单详情缺少收货人相关字段
- **THEN** 卡片显示「—」，不出现 `undefined` / `null`
