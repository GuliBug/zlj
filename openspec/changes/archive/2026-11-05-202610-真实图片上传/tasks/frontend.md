# tasks/frontend.md — 真实图片上传

## H5（frontend-h5）

- [x] T-F1 新增 `src/image.js`：compressImage(file) canvas 压缩（最长边 1080 / JPEG 0.72）【验证：E2E】
- [x] T-F2 `AftersaleNew.vue`：凭证位真实上传（隐藏 file input + 压缩追加 ≤3 + 缩略/删除 + 质量问题必传前端拦截）【FR-06-2｜验证：UI E2E UC-20】
- [x] T-F3 `AftersaleDetail.vue`：新增凭证区块（dataURL 缩略回显，兼容数组/字符串存储）【验证：UI E2E UC-21】
- [x] T-F4 `npm run build` 零报错【验证：构建】

## 管理端（frontend-web）

- [x] T-F5 新增 `src/utils/image.js`（同压缩策略）【验证：E2E】
- [x] T-F6 `product/edit.vue`：主图/详情图 URL 输入框替换为真实上传（≤3 / ≤6，缩略回显+删除）【FR-10-1｜验证：UI E2E WC-20】
- [x] T-F7 `npm run build` 零报错【验证：构建】

## E2E

- [x] T-F8 生成测试图片 `tests/e2e/assets/sample.png`；`ui-e2e-web.mjs` WC-20 上传真实主图并断言 images[0] 为 dataURL；`ui-e2e.mjs` UC-20 真实上传凭证、UC-21 断言详情页凭证 dataURL 回显【验证：UI E2E】
- [x] T-F9 UI E2E 全量回归（H5 22/22 + Web 22/22）并同步 docs/测试用例.md、docs/测试报告.md【验证：E2E】
