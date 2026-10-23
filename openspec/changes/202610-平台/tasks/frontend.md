## 1. 说明

- 「平台」能力为后端横切层（登录态/审计/告警/定时任务），无平台专属前端改动；审计与告警页面结构归「管理端」能力（views/audit/index.vue），见 202610-管理端 tasks/frontend.md。

## 2. 收尾门禁

- [x] 2.1 前端无改动，`cd frontend-web && npm run build` 与 `cd frontend-h5 && npm run build` 零报错（回归确认）
- [x] 2.2 `node tests/e2e/ui-e2e-web.mjs` 全部用例 PASS（含审计与告警页渲染断言）
