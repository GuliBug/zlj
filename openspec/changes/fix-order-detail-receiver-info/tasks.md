# Tasks: fix-order-detail-receiver-info

## 1. 实现

- [ ] 1.1 修正 `frontend-h5/src/pages/OrderDetail.vue` 的 `recv` computed：扁平字段组装 `{receiver, phone, addressText}`，`d` 为空时返回 `null`
- [ ] 1.2 简化模板「收货信息」卡片渲染，移除对不存在字段（`fullAddress/receiverName/region/detail` 等）的兜底链，空数据兜底为「—」

## 2. 验证

- [ ] 2.1 手工验证：EXPRESS 订单详情收货信息显示收货人/电话/完整地址，无 `undefined`（对应 H5-12 场景 1）
- [ ] 2.2 手工验证：收货信息字段缺失时显示「—」（对应 H5-12 场景 2）；PICKUP 订单详情回归自提门店卡片
- [ ] 2.3 质量门禁：`cd frontend-h5 && npm run build` 零报错
- [ ] 2.4 E2E 门禁：`node tests/e2e/e2e-mall.mjs` 全部 PASS
- [ ] 2.5 更新 `docs/测试用例.md` 与 `docs/测试报告.md`

## 3. 收尾

- [ ] 3.1 `openspec status --change fix-order-detail-receiver-info` 校验通过
- [ ] 3.2 人工确认后归档：delta 同步进 `openspec/specs/h5-client` 与 `openspec/specs/api-contract`，change 移入 `archive/`（禁止 AI 自动提交）
