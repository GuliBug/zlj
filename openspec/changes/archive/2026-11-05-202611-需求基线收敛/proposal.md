## Why

上一 change（`2026-11-05-202611-需求基线-v1.8`，已归档）确立了「需求资产双层存放」方案：`docs/需求/` 作稳定基线库 + change 的 `prd/` 作输入快照。经确认收敛为**PRD 单点存放**方案：

- 需求资产**只保留 PRD 一份**，存放于需求基线 change 的 `prd/` 目录（`openspec/changes/archive/2026-11-05-202611-需求基线-v1.8/prd/槟榔商城-PRD-v1.0.md`），遵循 change 内 `prd/` 目录规范。
- 删除 `docs/需求/` 整层，含 4 个配套交付件（H5 原型 / Web 管理端原型 / 时序图与流程图 / 需求总览）。**这不造成规格断链**：PRD 附录 A「原型思路（线框描述）」已完整承载原型的结构基准内容（H5 页面线框 + Web 管理端信息架构），主规格改为引用 PRD 章节锚点即可。
- 主规格 `H5-11` / `WA-18` 的「结构基准」由**文件系统路径引用**改为**PRD 章节锚点引用**（`PRD v1.0 附录 A.1` / `附录 A.2`），规格不再绑定任何具体文件路径。

本 change 是对上一 change 决策 FD1（双层存放）与 FD4（配套件一并入库）的**收敛修订**。

## What Changes

### 后端

- 无代码改动。后端规格的 FR 追溯锚点（`FR-xx-x`）指向 PRD 章节编号，锚点不变。

### 前端

- 删除 `docs/需求/` 目录及其下 5 个文件；PRD 唯一存放点为需求基线 change 的 `prd/`。
- 主规格 `H5买家端/frontend` 的 **H5-11**、主规格 `管理端/frontend` 的 **WA-18**：结构基准引用由文件路径改为 PRD 章节锚点，**结构基准描述逐字保留**。
- 进行中 change `202610-管理端`（design/frontend.md FD1、specs/管理端/frontend/spec.md WA-18）与 `202610-H5买家端`（specs/H5买家端/frontend/spec.md H5-11）同步改为章节锚点引用。
- `openspec/config.yaml`、`AGENTS.md`、`README.md` 的需求上游路径改为 PRD 实际存放位置。

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `H5买家端/frontend`：H5-11 页面结构对齐交互原型 —— 结构基准由 `docs/需求/槟榔商城-H5原型.html` 改为 `PRD v1.0 附录 A.1`；结构基准描述与两个场景逐字不变。
- `管理端/frontend`：WA-18 页面结构对齐 Web 管理端原型 —— 结构基准由 `docs/需求/槟榔商城-Web管理端原型.html` 改为 `PRD v1.0 附录 A.2`；结构基准描述与场景逐字不变。

## Impact

### 后端

- 无代码影响。`config.yaml` context 中需求来源路径更新，指向同一份 PRD 内容，FR 锚点不变。

### 前端

- 无代码影响，仅规格引用方式变更。
- 仓库移除 `docs/需求/` 目录（约 185 KB），PRD 单点存放于 change 的 `prd/`。

## 非目标

- 不修改 PRD v1.8 正文任何字符（版本、章节、FR 编号、附录 A 内容均不变）。
- 不改动任何业务代码、前端页面、后端接口。
- 不改动 `openspec/specs/接口契约/backend/spec.md`。
- 不新增、删除 OpenSpec 能力目录，不重构主规格库结构。
- 不回改已归档 change 的历史快照（`2026-11-05-202611-需求基线-v1.8` 的 FD1/FD4 决策与 tasks 记录保持原样，本 change 以「修订上一 change 决策」方式承接）。

## 修订记录

| 日期 | 摘要 | 提出方 |
| --- | --- | --- |
| 2026-11-05 | 需求基线收敛：PRD 单点存放于需求基线 change 的 `prd/`，删除 `docs/需求/` 整层（含 4 个配套交付件）；主规格 H5-11 / WA-18 结构基准改为 PRD 章节锚点引用 | 用户 |
