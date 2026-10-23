## ADDED Requirements

### Requirement: FR-03-1 加购与合并
加购 SHALL 合并同 SKU 数量；单 SKU 上限 999，购物车 SKU 种类上限 100，超限报「购物车已满」。

#### Scenario: 同 SKU 合并加购
- **WHEN** 对购物车已有 SKU 再次加购
- **THEN** 数量合并且单 SKU 上限 999

#### Scenario: 种类超限拒绝
- **WHEN** 购物车已有 100 种 SKU 再加购新 SKU
- **THEN** 系统拒绝并提示「购物车已满」

### Requirement: FR-03-3 数量上限与失效标识
购物车行 SHALL 返回实时库存与失效标识（SKU 下架/删除时 invalid=true）；支持更新数量/勾选、删除、全选、一键清除失效。

#### Scenario: 失效行标识
- **WHEN** 购物车内 SKU 对应商品被下架后查询购物车
- **THEN** 该行 invalid=true
