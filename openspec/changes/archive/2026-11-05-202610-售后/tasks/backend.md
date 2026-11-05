## 1. 买家端接口（AfterSaleController/AfterSaleService，需会员登录）

- [x] 1.1 GET /api/aftersales/reasons 原因枚举；POST /api/aftersales 申请（类型/原因/描述/凭证 1-3 张，质量问题凭证必填；仅 COMPLETED/SHIPPED 且签收 7 天内；每订单每 SKU 限 3 次）
- [x] 1.2 GET 列表（分页）与 /{no} 详情（含 flow 流转时间线）；POST /{no}/cancel 商家审核前撤销
- [x] 1.3 POST /{no}/return-logistics 退货退款态买家填寄回物流（WAIT_RETURN 流转）

## 2. 管理端接口（AdminAfterSaleController）

- [x] 2.1 GET /admin-api/aftersales：状态筛选、超 48h 未审核行 auditOverdue；GET /{no} 详情（买家申请信息 + flow）
- [x] 2.2 POST /{no}/audit 审核：意见必填；通过分支（仅退款直接退款/退货退款 → WAIT_RETURN）、拒绝 → REJECTED 可重新申请；留流转记录
- [x] 2.3 POST /{no}/confirm-return 退货退款确认收货（退款 + 库存回补）；POST /{no}/refund 幂等退款（退款单号 = 售后单号 + 序号；成功后积分按比例扣回）

## 3. 收尾门禁

- [x] 3.1 `cd backend && ./mvnw -q compile` 零报错
- [x] 3.2 `node tests/e2e/e2e-mall.mjs` 全部用例 PASS，docs/测试用例.md 与 docs/测试报告.md 已更新
