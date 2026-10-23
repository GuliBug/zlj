# Proposal: 2026-10-23 测试基建固化与代码规范加固

## Why
用户要求：① 双端 UI 功能页面全部真实跑通；② vitest 3.2.4 与 puppeteer 24.10.0 按参考工程版本固化并全流程跑通；③ H5 达到产线标准；④ 后端代码按规范审查。

## What Changes
- **测试依赖固化**：vitest@3.2.4 进入 h5/web devDependencies；puppeteer@24.10.0 进入 web devDependencies（对齐 qingguoji-mall），tests/e2e 改用本工程依赖，删除对参考项目 node_modules 的借用。
- **单元测试**：新增 h5 `api.test.js`（5 项）与 web `format.test.js`（7 项），`npm test` 接入双端。
- **UI 级 E2E 扩容**：新增 `tests/e2e/ui-e2e-web.mjs`（WC-01~11：登录、7 菜单页渲染、三步新增商品页违禁词实时拦截、全程无 JS 异常）；`ui-e2e.mjs` 断言收紧（UC-02 购物车数量/角标真实断言）。
- **产线标准**：双端补齐 ESLint 9 flat config + .prettierrc（对齐参考工程），修复全部 lint error；H5 index.html 补 theme-color/Apple meta/description/品牌 favicon（内联 SVG 印章）。
- **后端规范修复**：① ProductController 越层直用 Repository → 下沉 ProductService.saveArrivalRemind（补在售校验）；② batchShip 异常信息外泄 → 区分业务异常与系统异常。
- **测试文档**：测试报告新增「测试基建与质量门禁」章节；新增 `docs/后端代码审查报告.md`。

## Impact
- Affected specs: platform（新增 PF-06 质量门禁）、h5-client、web-admin
- 测试结果：接口级 89/89、UI 级 18/18、单测 12/12、双端 lint 0 error、双端构建通过
