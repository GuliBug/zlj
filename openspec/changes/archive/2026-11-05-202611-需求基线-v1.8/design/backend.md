# 后端设计（backend）

## Context

- 本 change 的实际改动全部在需求资产与文档引用层：无后端源码变更、无接口新增或变更、无数据模型变更。
- 后端规格（`openspec/specs/<能力>/backend/spec.md`）的业务口径以 PRD 的 FR 编号为追溯锚点，PRD **存放位置**变化不影响锚点编号与语义。

## Goals / Non-Goals

**Goals**

- 明确记录「本 change 无后端行为变更」，避免后续误以为存在后端 delta。
- 保持后端规格与 PRD 的追溯关系不断链（FR 编号不变）。

**Non-Goals**

- 不改动 `backend/` 下任何 Java 源码、资源或配置。
- 不改动 `openspec/specs/接口契约/backend/spec.md`。
- 不产生任何后端 delta 规格（`specs/<能力>/backend/` 下不建文件）。

## Decisions

### BD1：无后端 delta，本端文件仅作归属边界说明

- **方案**：不创建 `specs/<能力>/backend/spec.md`，本文件仅声明归属边界。
- **理由**：本次变更的两个 delta 均为**前端侧的文档引用路径修正**（H5-11、WA-18），归一文件侧能力。按 zlj-team schema「纯前端能力的另一端文件需注明无该端改动 + 归属边界，非空壳占位」处理。
- **取舍**：后端端在归档时无 delta 参与，主规格后端文件内容不变。

### BD2：`config.yaml` context 中的需求来源路径更新不构成契约变更

- **方案**：`openspec/config.yaml` 的 `需求来源` 字段由 `D:/projects/zlj/需求/槟榔商城-PRD-v1.0.md` 改为 `docs/需求/槟榔商城-PRD-v1.0.md`，指向同一份文件内容。
- **理由**：config 的 context 字段描述的是「需求从哪读」，属于工作流元信息，不是接口契约，也不是系统行为规格。文件内容与 FR 编号均未变化，追溯锚点不断链。
- **取舍**：context 文本随路径变化会被视为一次「配置修订」，需在提交说明中点明，以免误读为需求版本升级。

## Risks / Trade-offs

- **[风险] 后端规格的 FR 锚点引用 PRD 章节号**（如 `specs/订单/backend/spec.md` 中的 `FR-04-2`），若后续有人直接改写 PRD 正文而不改编号，锚点会失效。
  → **缓解**：PRD 正文修订必须走 change，且修订记录中明确 FR 编号是否顺延；归档时核对主规格锚点。

## Open Questions

（无）
