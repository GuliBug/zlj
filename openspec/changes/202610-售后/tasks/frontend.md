## 1. H5（frontend-h5）

- [x] 1.1 AftersaleNew.vue：售后说明卡、类型/原因下拉（原因来自接口）、描述选填、凭证 1-3 张勾选占位、提交提示工单号并跳详情、限 3 次提示
- [x] 1.2 AftersaleList.vue：工单卡片（工单号/类型/关联订单/申请时间/退款金额）、分页加载更多、空态引导
- [x] 1.3 AftersaleDetail.vue：工单信息 + flow 时间线（动作/时间/操作方/备注）、「不可删改」说明、申请态撤销入口（二次确认）
- [x] 1.4 收尾：`cd frontend-h5 && npm run build` 零报错；`node tests/e2e/ui-e2e.mjs` 全 PASS

## 2. Web 管理端（frontend-web）

- [x] 2.1 views/aftersale/index.vue：工单列表（状态筛选、超 48h 高亮）、审核弹窗（意见必填、通过/拒绝）、确认收货、退款（二次确认、金额只读）、流转时间线实时追加
- [x] 2.2 收尾：`cd frontend-web && npm run build` 零报错；`node tests/e2e/ui-e2e-web.mjs` 全 PASS（售后审核按钮与弹窗断言）

## 3. 缺口补齐补录（2026-10-26，FD6）

- [x] 3.1 AftersaleDetail.vue：`RETURN_REFUND + WAIT_RETURN` 态展示「填写寄回物流」卡（公司/单号必填，提交后刷新转退款中）；已填时展示「寄回物流」卡（公司 · 单号）【FR-13-2｜验证：UI E2E UC-22 PASS，填写后状态转退款中+流转记录含「买家已寄回商品」+寄回物流卡回显】
- [x] 3.2 收尾：`cd frontend-h5 && npm run build` 零报错；`node tests/e2e/ui-e2e.mjs` 23/23 PASS（含新增 UC-22）；docs/测试用例.md 与 docs/测试报告.md 已同步
