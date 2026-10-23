# 商品 Specification — frontend

## Purpose
商品目录前端侧：H5 商品详情与结算页的合规文案展示与缺货置灰交互（工程 frontend-h5）。

## Requirements

### Requirement: FR-02-2 商品详情
H5 商品详情 SHALL 展示主图、名称、SKU 选择器、价格、库存状态、固定警示语「长期过量咀嚼槟榔，有害口腔健康」与产地；SKU 缺货置灰不可选。

#### Scenario: 警示语强制展示
- **WHEN** 打开任意商品详情
- **THEN** 展示固定警示语（后端字段硬编码，不依赖配置）

### Requirement: FR-02-5 未成年人提示
详情页与结算页 SHALL 展示固定文案「不向未成年人销售」（硬编码）。

#### Scenario: 未成年人提示展示
- **WHEN** 打开商品详情页或确认订单页
- **THEN** 固定展示「不向未成年人销售」文案
