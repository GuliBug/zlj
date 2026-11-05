## 1. 需求资产入库

- [x] 1.1 建立 `docs/需求/` 稳定基线库，入库 PRD v1.8（`槟榔商城-PRD-v1.0.md`）【验证：文件存在且 40239 字节，与源文件逐字节一致】
- [x] 1.2 入库配套交付件：`槟榔商城-H5原型.html`、`槟榔商城-Web管理端原型.html`、`槟榔商城-时序图与流程图.html`、`需求总览-槟榔商城.md`【验证：4 个文件均在 `docs/需求/`，双击可离线打开】
- [x] 1.3 建立本 change 的 `prd/` 输入快照，存放 PRD 单文件副本【验证：`openspec/changes/202611-需求基线-v1.8/prd/槟榔商城-PRD-v1.0.md` 存在，与 `docs/需求/` 版本哈希一致】

## 2. 引用路径修正

- [x] 2.1 `openspec/config.yaml` 的 `需求来源` 改为 `docs/需求/槟榔商城-PRD-v1.0.md`【验证：全仓检索 `D:/projects/zlj` 无残留】
- [x] 2.2 `AGENTS.md` 需求上游改为仓库内路径【验证：同上】
- [x] 2.3 `README.md` 需求上游改为仓库内路径【验证：同上】
- [x] 2.4 主规格 `H5买家端/frontend/spec.md` 的 H5-11 原型基准路径修正（经本 change delta 同步）【验证：delta 为 MODIFIED 整块复制，仅路径字符串变化】
- [x] 2.5 主规格 `管理端/frontend/spec.md` 的 WA-18 原型基准路径修正（经本 change delta 同步）【验证：同上】

## 3. 收尾

- [x] 3.1 `openspec validate --all` 全绿，无 ERROR【验证：validate 输出无 ERROR】
- [x] 3.2 引用可解析性自检：全仓检索 `D:/projects/zlj` 与 `projects/zlj/需求`，确认无失效引用【验证：检索结果为空】
- [x] 3.3 PRD 内容一致性自检：入库存本与源文件哈希一致，未发生任何内容改写【验证：两文件 SHA-256 相同】

> 说明：本 change 为**纯文档变更**，不触碰 `backend/`、`frontend-web/`、`frontend-h5/` 任何源码，故不触发 `mvnw compile` / `npm run build` / `node tests/e2e/e2e-mall.mjs` 三项代码门禁；收尾门禁以 `openspec validate` + 引用可解析性 + 内容哈希一致性三项替代。
