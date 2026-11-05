## 1. 需求资产收敛

- [x] 1.1 删除 `docs/需求/` 整层（PRD 副本 + H5 原型 + Web 管理端原型 + 时序图与流程图 + 需求总览）【验证：`docs/需求/` 目录不再存在】
- [x] 1.2 确认 PRD 单点存放于需求基线 change 的 `prd/` 目录【验证：`openspec/changes/archive/2026-11-05-202611-需求基线-v1.8/prd/槟榔商城-PRD-v1.0.md` 存在，SHA-256 = `CF94D88208B7EADD8521CE3B42785484D8D9BB987273B17B86B14DE40EDC83C0`（与源文件一致）】
- [x] 1.3 PRD 正文零改动确认【验证：入库副本与微信交付源文件哈希一致，未发生任何内容改写】

## 2. 主规格结构基准改为章节锚点

- [x] 2.1 主规格 `H5买家端/frontend/spec.md` 的 H5-11 结构基准改为「PRD v1.0 附录 A.1」（经本 change delta 同步）【验证：delta 为 MODIFIED 整块复制，结构基准描述与两个场景逐字保留，仅基准引用变化】
- [x] 2.2 主规格 `管理端/frontend/spec.md` 的 WA-18 结构基准改为「PRD v1.0 附录 A.2」（经本 change delta 同步）【验证：同上】
- [x] 2.3 进行中 change `202610-管理端`：`specs/管理端/frontend/spec.md`（WA-18）与 `design/frontend.md`（FD1）同步改为章节锚点【验证：两处均已改为附录 A.2】
- [x] 2.4 进行中 change `202610-H5买家端`：`specs/H5买家端/frontend/spec.md`（H5-11）同步改为章节锚点【验证：已改为附录 A.1】
- [x] 2.5 进行中 change 的 `proposal.md` 修订记录与 `tasks/frontend.md` 追加本次收敛条目【验证：两个 change 各有 2026-11-05 两条修订记录】

## 3. 全局路径声明同步

- [x] 3.1 `openspec/config.yaml` 的 `需求来源` 指向 PRD 实际存放位置【验证：与 AGENTS.md / README.md 三处一致】
- [x] 3.2 `AGENTS.md` 需求上游路径同步【验证：同上】
- [x] 3.3 `README.md` 需求上游路径同步【验证：同上】

## 4. 收尾

- [x] 4.1 `openspec validate --all` 全绿，无 ERROR【验证：validate 输出 Totals 全 passed / 0 failed】
- [x] 4.2 失效引用自检：全仓检索 `D:/projects/zlj`、`docs/需求`，确认无残留活引用【验证：检索结果为空】
- [x] 4.3 PRD 内容一致性自检：入库存本与源文件 SHA-256 一致【验证：哈希相同】

> 说明：本 change 为**纯文档变更**，不触碰 `backend/`、`frontend-web/`、`frontend-h5/` 任何源码，故不触发 `mvnw compile` / `npm run build` / `node tests/e2e/e2e-mall.mjs` 三项代码门禁；收尾门禁以 `openspec validate` + 引用可解析性 + 内容哈希一致性三项替代。
