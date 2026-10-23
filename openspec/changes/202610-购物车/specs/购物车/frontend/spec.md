## ADDED Requirements

### Requirement: FR-03-2 实时合计与库存标识
H5 购物车 SHALL 实时展示勾选商品合计金额与库存不足标识（进入购物车页时刷新库存）。

#### Scenario: 库存不足标识
- **WHEN** 某 SKU 可用库存低于勾选数量并进入购物车页
- **THEN** 该行展示库存不足标识

### Requirement: FR-03-4 失效商品
已下架/失效商品 SHALL 置灰标识「商品已失效」，不可结算，支持一键清除失效商品。

#### Scenario: 一键清除失效
- **WHEN** 点击清除失效商品
- **THEN** 所有 invalid 行被删除
