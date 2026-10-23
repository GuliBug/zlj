## Context

Web 管理端门店页（frontend-web/src/views/store/index.vue）：「＋ 新增门店」按钮（openEdit(null)）、门店编辑弹窗（新增/编辑共用 submitStore）、核销表单门店下拉（verifyForm.storeId el-select）、提示语「H5 端买家可切换门店；商品、核销码均跟随所属门店」。H5 已单店（此前迭代完成）。

## Goals / Non-Goals

**Goals:**

- 门店页单店化：移除新增入口、固定核销门店、文案对齐。

**Non-Goals:**

- 不动 H5；不动核销校验逻辑；不删门店编辑能力。

## Decisions

### FD1 移除新增入口，编辑弹窗仅服务默认门店

- 删除「＋ 新增门店」按钮；编辑弹窗标题固定「编辑门店信息」，移除新增分支判断。
- `createStore` API 封装保留在 api/store.js（对齐后端契约），页面不再调用。

### FD2 核销门店固定默认门店

- 移除 el-select 下拉；`verifyForm.storeId` 提交时取门店列表首项 id（单店模式下列表仅一个元素，无需额外配置）。
- 提示文案改为「单店模式：全站仅默认门店，不支持新增或切换；门店信息变更即时同步 H5」。

### FD3 契约疑问章节

- 无疑问：接口结构不变，已在 api.md 记录口径（POST /admin-api/stores 契约保留、前端不再使用）。

## Risks / Trade-offs

- [后端种子若被改为多店，页面无新增入口会显得受限] → 属预期行为（单店模式），proposal 非目标已声明。
