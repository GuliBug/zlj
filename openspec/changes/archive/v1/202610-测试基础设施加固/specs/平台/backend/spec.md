### Requirement: PF-06 质量门禁
工程 SHALL 建立三层测试体系并作为回归门禁：单元测试（vitest 3.2.4，双端 npm test）、UI 级端到端（puppeteer 24.10.0 真实浏览器，H5 + Web 管理端）、接口级端到端（89 断言）；双端 SHALL 通过 ESLint（flat config）与 Prettier 检查（0 error）。测试依赖版本 SHALL 与参考工程对齐并在 package.json 固化。

#### Scenario: 回归门禁全绿
- **WHEN** 执行 npm test、node tests/e2e/ui-e2e.mjs、ui-e2e-web.mjs、e2e-mall.mjs
- **THEN** 全部通过且结果自动并入 docs/测试用例.md 与 docs/测试报告.md
