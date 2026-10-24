# tasks/frontend.md — 提交上架生效

## 管理端（frontend-web）

- [x] T-F1 `tests/e2e/ui-e2e-web.mjs` WC-20 断言加强：命中新商品行的 `status === 'ON'`（BUG-09 回归锚点）【FR-10-1｜验证：UI E2E】
- [x] T-F2 质量门禁：UI E2E 全量回归（`node tests/e2e/ui-e2e-web.mjs` 22/22 PASS，WC-20 详情 status=ON；截图归档 `tests/e2e/screenshots/web/`）【验证：E2E】
- [x] T-F3 产品代码无改动，`npm run build` 不涉及本次变更（测试脚本不参与构建）【验证：说明】

## H5（frontend-h5）

- [x] T-F4 无代码改动；UI E2E 全量回归 22/22 PASS（提交上架商品 H5 即时可见语义由后端保证）【验证：E2E】
