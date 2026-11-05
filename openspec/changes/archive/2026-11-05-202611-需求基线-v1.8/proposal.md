## Why

`openspec/config.yaml` 与 `AGENTS.md` 声明的需求上游为 `D:/projects/zlj/需求/槟榔商城-PRD-v1.0.md`，该路径在当前开发机上**不存在**（`D:\projects\zlj` 目录整体缺失），导致：

- 需求权威来源不可访问，规格中「以原型为结构基准」这类 SHALL 条款无法验证；
- 主规格 `H5买家端/frontend/spec.md`（H5-11）与 `管理端/frontend/spec.md`（WA-18）引用的原型文件路径同样失效；
- 新成员或 Agent 接手时无法定位需求基线，SDD 流程的「需求上游」环节断链。

本 change 将 PRD v1.8 及其配套交付件固化进仓库，使需求上游成为仓库内可追溯、可版本管理的稳定资产。

## What Changes

### 后端

- 无代码改动。后端规格的业务口径以 PRD v1.8 的 FR 编号为追溯锚点，PRD 位置变更不影响后端行为定义。

### 前端

- 需求基线入库：PRD v1.8 及配套交付件落入 `docs/需求/`（稳定基线库），PRD 副本落入本 change 的 `prd/`（变更输入快照）。
- 引用修正：`H5-11` 与 `WA-18` 两条 requirement 的原型基准路径，由失效的 `D:/projects/zlj/需求/...` 改为仓库内相对路径 `docs/需求/...`。
- 全局引用修正：`openspec/config.yaml`（需求来源）、`AGENTS.md`（需求上游）、`README.md`（需求上游）三处指向同一稳定路径。

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `H5买家端/frontend`：H5-11 页面结构对齐交互原型 —— 仅修正原型基准文件路径，结构基准描述（品牌导航/警示条/hero 与四宫格/分档 chips/TabBar 四项）保持不变。
- `管理端/frontend`：WA-18 页面结构对齐 Web 管理端原型 —— 仅修正原型基准文件路径，侧栏 7 菜单/主色/工作台指标卡/新增商品三步流程/门店页核销记录等结构基准描述保持不变。

## Impact

### 后端

- 无代码影响。`config.yaml` 的 context 文本更新后，后端规格追溯锚点（FR-xx-x 编号）不变。

### 前端

- 无代码影响，仅规格引用路径变更。
- 仓库新增 `docs/需求/` 目录（5 个文件，约 185 KB），随仓库版本管理。

## 非目标

- 不修改 PRD v1.8 正文内容（版本、章节、FR 编号均不变）。PRD 正文调整需另行立项 change。
- 不改动任何业务代码、前端页面、后端接口。
- 不新增、不删除任何接口；接口契约主规格 `接口契约/backend/spec.md` 本次不动。
- 不新增 OpenSpec 能力目录，不重构主规格库结构。

## 修订记录

| 日期 | 摘要 | 提出方 |
| --- | --- | --- |
| 2026-11-05 | 需求基线 v1.8 入库（PRD + 原型 + 时序图 + 需求总览），并修正 config/AGENTS/README 与主规格中的失效需求路径 | 用户 |
