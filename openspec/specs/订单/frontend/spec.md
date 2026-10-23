# 订单 Specification — frontend

## Purpose
交易闭环前端侧：订单详情页信息展示。

## Requirements

### Requirement: FR-05-3 订单详情
详情 SHALL 展示订单号、明细、金额明细、支付方式、收货信息/自提门店、物流单号、状态时间轴。

#### Scenario: 自提订单详情信息完整
- **WHEN** 买家进入 PICKUP_WAIT 自提订单详情
- **THEN** 展示订单号、商品明细、金额明细、自提门店、动态核销码与状态时间轴
