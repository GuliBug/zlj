## 1. H5（frontend-h5）

- [x] 1.1 Stores.vue：门店信息纯展示（名称/地址/电话/营业时间/状态，CLOSED 半透明「休息中」）、运营后台维护说明、进入时 applyDefaultStore 同步全局默认门店
- [x] 1.2 结算自提块与核销码页固定展示默认门店（单店锁定，无切换）
- [x] 1.3 收尾：`cd frontend-h5 && npm run build` 零报错；`node tests/e2e/ui-e2e.mjs` 全 PASS

## 2. Web 管理端（frontend-web）

- [x] 2.1 views/store/index.vue：单店列表（主店标签/状态）、编辑弹窗（名称/地址/电话/营业时间/状态）、无新增入口（单店模式说明）
- [x] 2.2 核销操作面板（订单号 + 6 位码、成功/失败提示条）与核销记录列表（关键字/日期过滤、待自提「扫码核销」预填）
- [x] 2.3 收尾：`cd frontend-web && npm run build` 零报错；`node tests/e2e/ui-e2e-web.mjs` 全 PASS
