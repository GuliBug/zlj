## 1. SKU 与库存操作（AdminStockController/StockService）

- [x] 1.1 GET /admin-api/skus：实物/可用/预占/冻结四维 + 安全阈值 + 状态（NORMAL|FROZEN），keyword 检索分页
- [x] 1.2 POST /admin-api/stock/inbounds 入库单（数量>0、批次号必填，生成 IN 流水）与入库单列表
- [x] 1.3 POST /admin-api/stock/freeze 冻结/释放（FREEZE|RELEASE + 原因；冻结可用清零、预占不受影响，生成流水）
- [x] 1.4 POST /admin-api/stock/threshold 阈值修改（默认 50，留审计）；GET /admin-api/stock/flows 流水检索（8 类类型 + 关联单号）

## 2. 盘点与核对

- [x] 2.1 POST /admin-api/stock/stocktakes 盘点单（差异自动计算，PENDING_CONFIRM）；GET 列表（系统数/实盘/差异/提交人/确认人）
- [x] 2.2 POST /admin-api/stock/stocktakes/{id}/confirm 二级确认（提交人≠确认人，违反 code=500；确认后按差异修正并生成 ADJUST 流水）
- [x] 2.3 StockReconcileTask 日终核对：实物=Σ流水，差异登记 RECON_DIFF 告警；可用跌破阈值触发 STOCK_ALERT（同 SKU 24h 去重）

## 3. 收尾门禁

- [x] 3.1 `cd backend && ./mvnw -q compile` 零报错
- [x] 3.2 `node tests/e2e/e2e-mall.mjs` 全部用例 PASS，docs/测试用例.md 与 docs/测试报告.md 已更新
