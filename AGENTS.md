# AGENTS.md — 正隆记槟榔商城 OpenSpec 协作约定

本仓库用 **OpenSpec（spec-driven schema）** 管理 SDD 流程与代码。规格权威来源是 `openspec/`，需求上游是 `D:/projects/zlj/需求/槟榔商城-PRD-v1.0.md`。

> 本文件是约定摘要；可执行的规则以 `openspec/config.yaml` 为准（context / rules / operations）。

## 0. 工作流（每个需求/缺陷/重构都走 change）

1. **起 change**：`openspec new change "<kebab-name>"`（例如 `add-invoice`、`fix-stock-overflow`）。
2. **写产物**（按依赖顺序）：`proposal.md` → `specs/<cap>/spec.md`（增量 delta）→ `design.md` → `tasks.md`。
3. **实现（apply）**：仅在明确进入实现阶段后改代码；先读 `openspec/specs/<cap>/spec.md` 作为当前事实。
4. **校验（verify）**：本地跑质量门禁（后端构建 / 前端 build / E2E），全绿再勾任务。
5. **归档（archive）**：把 delta 同步进主规格库并移入 `openspec/changes/archive/`。

> 规划与实现是两段：写完规划产物先停下等用户确认，再进入 apply。

## 1. 产物语言与风格

- 所有规划产物用**中文**撰写；技术标识符（类名/接口路径/字段）保留英文原文。
- `specs` 用 `### Requirement:` + `#### Scenario:`（WHEN/THEN）描述**可观测行为**，不写实现细节；保留原 PRD 编号（如 `### Requirement: FR-04-2 下单幂等`）作为追溯锚点。
- 增量用 `## ADDED/MODIFIED/REMOVED/RENAMED Requirements` 标记；`MODIFIED` 须带完整更新内容。

## 2. 契约权威

- `openspec/specs/api-contract/spec.md` 是前后端**唯一契约来源**。
- 前端发现接口与契约不符 → 在对应 change 的 `design.md`「契约疑问」章节记录，后端确认后更新 `api-contract` 主规格；**禁止前端直接改主规格**。

## 3. 质量门禁（每个 change 的收尾任务必须包含）

- 后端：`cd backend && ./mvnw -q compile`（或 `mvnw.cmd`）零报错。
- 前端-web / 前端-h5：`npm run build` 零报错。
- E2E：`node tests/e2e/e2e-mall.mjs` 全部用例 PASS，并更新 `docs/测试用例.md` 与 `docs/测试报告.md`。

## 4. 提交纪律

- **禁止 AI 自动 commit / push**；门禁通过后，由人工决定是否提交。
- 提交信息建议 `[模块] 动作：说明`（如 `[backend] feat: 售后流转记录`）。

## 5. 工程边界

- `backend/`（Spring Boot，8080）、`frontend-web/`（5173）、`frontend-h5/`（10086）、`tests/`、`docs/`、`openspec/`；后端不托管前端页面。
- 合规底线硬编码：警示语/未成年人提示/违禁词校验/审计日志（见 config.yaml context）。
- 已移除模块勿再恢复：RBAC/合规中心（v1.2）、售卖区域限制（v1.4）、优惠券（v1.5）、商品资质字段（v1.8）。
