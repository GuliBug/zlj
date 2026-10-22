### Requirement: O-05 订单详情返回商品明细
订单详情接口（GET /api/orders/{no} 与管理端详情）SHALL 返回 `items` 数组，每项含 `skuId/name/image/flavor/spec/price/quantity/subtotal`，用于买家与管理端渲染商品清单。

### Requirement: CT-04 搜索匹配品牌
买家商品搜索 keyword SHALL 同时匹配商品名称、SKU 条码与品牌（PRD：按名称/品牌搜索）。

### Requirement: ST-04 门店营业时间
门店实体 SHALL 含 `operatingHours`（营业时间）字段；门店列表与详情返回该字段；管理端门店编辑支持维护。
