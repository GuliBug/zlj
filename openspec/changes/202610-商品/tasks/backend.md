## 1. 买家端接口（ProductController/ProductService）

- [x] 1.1 GET /api/categories 分类树；GET /api/products 列表与搜索（keyword 匹配名称/条码/品牌，仅上架可见，分页）
- [x] 1.2 GET /api/products/{id} 详情：skus（口味/规格/条码/售价/可用库存/销量）、合规字段 warnText/minorText 硬编码返回
- [x] 1.3 POST /api/products/{id}/remind 到货提醒：需会员登录，在售商品拒绝

## 2. 合规底线

- [x] 2.1 内置违禁词库；商品保存与上架前校验命中即阻断并列出命中词；POST /admin-api/check-banned-words 实时校验

## 3. 管理端接口（AdminProductController）

- [x] 3.1 商品新增/编辑：条码唯一、售价>0、库存≥0；改价对未支付订单不生效；变更留审计
- [x] 3.2 POST /admin-api/products/status 单个与批量上下架：即时生效、留审计
- [x] 3.3 GET/POST /admin-api/categories 分类查询与新增

## 4. 收尾门禁

- [x] 4.1 `cd backend && ./mvnw -q compile` 零报错
- [x] 4.2 `node tests/e2e/e2e-mall.mjs` 全部用例 PASS，docs/测试用例.md 与 docs/测试报告.md 已更新
