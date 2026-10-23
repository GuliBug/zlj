## 1. 买家端接口（StoreController）

- [x] 1.1 GET /api/stores：门店数组（name/address/phone/operatingHours/status），单店模式返回单元素（正隆记总号）

## 2. 管理端接口（AdminStoreController）

- [x] 2.1 GET/POST/PUT /admin-api/stores：门店列表/新增（契约保留，单店模式前端无入口）/编辑（名称/地址/电话/营业时间/状态启停，即时生效）
- [x] 2.2 POST /admin-api/verify 核销：校验订单 PICKUP_WAIT + 当前/上一分钟窗口码 + 门店匹配；成功 → COMPLETED、库存实扣、核销留痕、时间轴节点；失败返回明确原因
- [x] 2.3 GET /admin-api/verify-records 核销记录分页（店员/时间/订单/门店）

## 3. 收尾门禁

- [x] 3.1 `cd backend && ./mvnw -q compile` 零报错
- [x] 3.2 `node tests/e2e/e2e-mall.mjs` 全部用例 PASS（含接口级核销全流程），docs/测试用例.md 与 docs/测试报告.md 已更新
