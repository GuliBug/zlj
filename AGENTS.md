# AGENTS.md — 正隆记槟榔商城 OpenSpec 协作约定

本仓库用 **OpenSpec（spec-driven schema）** 管理 SDD 流程与代码。规格权威来源是 `openspec/`，需求上游是 `D:/projects/zlj/需求/槟榔商城-PRD-v1.0.md`。

> 本文件是约定摘要；可执行的规则以 `openspec/config.yaml` 为准（context / rules / operations）。

## 0. 工作流（每个需求/缺陷/重构都走 change）

1. **起 change**：先 `openspec new change "<kebab-en>"` 生成骨架，再把目录重命名为「YYYYMM-中文标题」（CLI 创建仅允许小写字母/数字/连字符，需改名；`list`/`show`/`status`/`validate`/归档均识别中文目录名）。例如 `202610-已售数量累计`。
2. **写产物**（schema: zlj-team，按依赖顺序，产物按端拆分文件）：
   - `proposal.md`（共享，定稿冻结+文末修订记录）
   - `api.md`（存在接口新增/变更时必写，字段级契约；**先改 api.md 再动代码**；纯前端改动写明无接口变更）
   - `specs/<能力>/<端>/spec.md`（增量 delta，端为 backend/frontend）
   - `design/backend.md`、`design/frontend.md`（FD 文件内部分 H5 / 管理端小节）
   - `tasks/backend.md`、`tasks/frontend.md`
   - 小缺陷修复类 change 允许只写涉及的端文件，缺省端文件不建空壳。
3. **实现（apply）**：仅在明确进入实现阶段后改代码；先读 `openspec/specs/<能力>/<端>/spec.md` 作为当前事实。
4. **校验（verify）**：本地跑质量门禁（后端构建 / 前端 build / E2E），全绿再勾任务。
5. **归档（archive）**：把 delta 同步进主规格库、api.md 定稿契约同步进「接口契约」主规格，并移入 `openspec/changes/archive/`。

> 规划与实现是两段：写完规划产物先停下等用户确认，再进入 apply。

## 1. 产物语言与风格

- 所有规划产物用**中文**撰写；技术标识符（类名/接口路径/字段）保留英文原文。
- `specs` 用 `### Requirement:` + `#### Scenario:`（WHEN/THEN）描述**可观测行为**，不写实现细节；保留原 PRD 编号（如 `### Requirement: FR-04-2 下单幂等`）作为追溯锚点。
- 增量用 `## ADDED/MODIFIED/REMOVED/RENAMED Requirements` 标记；`MODIFIED` 须带完整更新内容。
- 能力目录（`specs/<cap>/`）与 change 目录一样用中文命名（现有：会员/商品/购物车/订单/售后/库存/门店/管理端/H5买家端/平台/接口契约）；change 内 delta 的 `specs/<cap>/` 目录名必须与主规格库一致，spec.md 落在对应端子目录（backend/frontend），否则归档同步对不上。
- 工作流 schema 为 `openspec/schemas/zlj-team/`（产物定义 + 模板，模板含强制章节：非目标/决策编号 BD-FD/风险取舍/修订记录）；端语义：backend = 系统行为，frontend = 页面渲染与交互。

## 2. 契约权威

- `openspec/specs/接口契约/backend/spec.md` 是前后端**唯一契约来源**。
- 前端发现接口与契约不符 → 在对应 change 的 `design/frontend.md`「契约疑问」章节记录，后端确认后更新 api.md 与 `接口契约` 主规格；**禁止前端直接改主规格**。

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
