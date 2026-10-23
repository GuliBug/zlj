# 购物车 Specification — backend

## Purpose
购物车后端侧：加购合并策略、数量上限、失效清理的系统行为与口径，覆盖 FR-03 的数据规则。

## Requirements

### Requirement: FR-03-1 购物车基础操作
系统 SHALL 支持加购、改数量、删除、勾选/取消、全选；同 SKU 重复加购数量累加，单 SKU 上限 999。

#### Scenario: 重复加购合并
- **WHEN** 同 SKU 再次加购
- **THEN** 数量累加而非新增一行

### Requirement: FR-03-3 数量上限
单账号购物车 SKU 种类上限 100，超限提示「购物车已满」。

#### Scenario: 超限拦截
- **WHEN** 已有 100 种 SKU 再加购新品种
- **THEN** 加购失败提示购物车已满
