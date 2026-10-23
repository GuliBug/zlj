## 1. 登录态（framework/security）

- [x] 1.1 X-Token 双前缀（MBR-/ADM-）7 天有效；TokenService 签发校验；@RequireLogin + LoginInterceptor 按 MEMBER/ADMIN 类型拦截
- [x] 1.2 POST /admin-api/login：账密登录返回 ADM- 令牌；连续 5 次失败锁定 30 分钟

## 2. 审计与告警

- [x] 2.1 AuditService/AuditLog：改价/上下架/积分调整/人工调分/盘点确认/阈值修改统一留痕（操作人/时间/IP/变更前后值），不可删改
- [x] 2.2 AlertService/Alert：DUPLICATE_PAY/ABNORMAL_AMOUNT/STATUS_ROLLBACK/RECON_DIFF/STOCK_ALERT 五类告警（STOCK_ALERT 同 SKU 24h 去重），只增不改
- [x] 2.3 GET /admin-api/audit-logs（module 筛选）与 GET /admin-api/alerts 分页查询

## 3. 定时任务

- [x] 3.1 OrderTimeoutTask：30s 扫描超 15 分钟待支付订单，关单前二次校验支付状态，关闭并释放预占
- [x] 3.2 StockReconcileTask：日终核对实物=Σ流水，差异登记 RECON_DIFF 告警

## 4. 收尾门禁

- [x] 4.1 `cd backend && ./mvnw -q compile` 零报错
- [x] 4.2 `node tests/e2e/e2e-mall.mjs` 全部用例 PASS（含幂等/重复支付告警/超时关单断言），docs/测试用例.md 与 docs/测试报告.md 已更新
