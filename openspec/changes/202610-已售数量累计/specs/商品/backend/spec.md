# Delta: 202610-已售数量累计 → 商品

## ADDED Requirements

### Requirement: FR-02-6 已售数量累计

商品与 SKU 的「已售」数量（`sales`）必须随真实成交累计，口径为**库存实扣数量**：

#### Scenario: 快递订单支付后已售累计
- **WHEN** 买家对 EXPRESS 订单完成支付，系统实扣库存
- **THEN** 订单内各 SKU 的 `sales` 与所属商品的 `sales` 原子增加对应购买数量
- **AND** 买家端商品列表与详情接口返回的 `sales` 反映累计值

#### Scenario: 自提订单核销后已售累计
- **WHEN** 自提订单（PICKUP_WAIT）被门店核销完成，系统实扣库存
- **THEN** 各 SKU 的 `sales` 与所属商品的 `sales` 原子增加对应购买数量

#### Scenario: 取消订单不影响已售
- **WHEN** 买家取消待支付订单（仅释放预占，未发生实扣）
- **THEN** 相关 SKU 与商品的 `sales` 不变

#### Scenario: 退款不递减已售
- **WHEN** 售后退款完成，库存回补
- **THEN** 相关 SKU 与商品的 `sales` 保持不变（「已售」为累计成交口径，不随退款波动）
