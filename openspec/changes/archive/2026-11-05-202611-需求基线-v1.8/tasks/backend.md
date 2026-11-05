## 1. 归属边界

本 change 为**纯文档变更**，无后端代码改动。

- **无 `specs/<能力>/backend/` delta**：本次两个 delta 均为前端侧文档引用路径修正（H5-11、WA-18），归前端文件侧能力；后端主规格文件内容不变。
- **无 `api.md` 接口变更**：`openspec/specs/接口契约/backend/spec.md` 本次不动，接口契约无新增/变更/废弃。
- **无后端源码改动**：`backend/` 下 Java 源码、资源、配置一律不动。
- **FR 追溯锚点不变**：后端规格中的 `FR-xx-x` 锚点编号与语义保持不变，PRD 仅存放位置变更。

## 2. 后端相关引用核对

- [x] 2.1 核对 `openspec/config.yaml` 的 `需求来源` 路径修正后，后端规格的 FR 锚点仍指向同一份 PRD 内容【验证：PRD 内容哈希未变，FR 编号未变】
- [x] 2.2 收尾：本端无代码改动，不触发 `mvnw compile`；后端产物一致性由 `openspec validate --all` 与需求文件哈希自检覆盖
