# 购物车 Specification

## Purpose
购物车：加购/数量步进/勾选/删除/全选/失效清理/合计，合并策略与上限，覆盖 FR-03。

## ADDED Requirements

### Requirement: FR-03-1 购物车基础操作
系统 SHALL 支持加购、改数量、删除、勾选/取消、全选；同 SKU 重复加购数量累加，单 SKU 上限 999。

#### Scenario: 重复加购合并
- **WHEN** 同 SKU 再次加购
- **THEN** 数量累加而非新增一行

### Requirement: FR-03-2 实时合计与库存标识
购物车 SHALL 实时展示勾选商品合计金额与库存不足标识（进入购物车页时刷新库存）。

### Requirement: FR-03-3 数量上限
单账号购物车 SKU 种类上限 100，超限提示「购物车已满」。

#### Scenario: 超限拦截
- **WHEN** 已有 100 种 SKU 再加购新品种
- **THEN** 加购失败提示购物车已满

### Requirement: FR-03-4 失效商品
已下架/失效商品 SHALL 置灰标识「商品已失效」，不可结算，支持一键清除失效商品。

#### Scenario: 一键清除失效
- **WHEN** 点击清除失效商品
- **THEN** 所有 invalid 行被删除
