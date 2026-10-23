## 1. 下单与支付（OrderController/OrderService）

- [x] 1.1 POST /api/orders：服务端计价（运费/积分抵扣/金额风控）、幂等键 5 分钟去重、库存预占同一事务（乐观锁，缺货返回 SKU 与剩余可购量、整单回滚）
- [x] 1.2 POST /{no}/pay（模拟支付、重复支付拒绝并告警）、/cancel（仅 WAIT_PAY，释放预占）、/confirm（仅 SHIPPED）
- [x] 1.3 OrderTimeoutTask：30s 扫描超 15 分钟待支付订单，关单前二次校验支付状态，关闭并释放预占

## 2. 查询与核销码

- [x] 2.1 GET /api/orders 状态筛选倒序分页（行含 items 摘要）；GET /{no} 详情（金额明细/收货或自提信息/物流/时间轴含售后合并节点/aftersale 摘要）
- [x] 2.2 GET /{no}/verify-code：6 位动态核销码（分钟窗口哈希，expiresInSec ≤ 60）

## 3. 管理端接口（AdminOrderController）

- [x] 3.1 GET /admin-api/orders：状态/订单号/脱敏手机号筛选、超 24h 未发货行 shipOverdue
- [x] 3.2 POST /{no}/ship 发货、/batch-ship CSV 批量发货（失败行回显原因）、GET /export 订单 CSV 导出

## 4. 收尾门禁

- [x] 4.1 `cd backend && ./mvnw -q compile` 零报错
- [x] 4.2 `node tests/e2e/e2e-mall.mjs` 全部用例 PASS，docs/测试用例.md 与 docs/测试报告.md 已更新
