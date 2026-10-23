# Tasks: 202610-管理端商品图片回退

## 1. 实现

- [x] 1.1 新增 `frontend-web/src/utils/placeholder.js`，导出品牌占位图常量 `PLACEHOLDER_IMG`
- [x] 1.2 `views/product/index.vue`：改用共享常量；`el-image` 增加 `#error` 插槽回退占位图；`preview-src-list` 兜底
- [x] 1.3 `views/product/edit.vue`：主图/详情图预览 `el-image` 增加 `#error` 插槽回退占位图

## 2. 验证

- [ ] 2.1 手工：商品列表缩略图全部有图（原图或占位），无「加载失败」（WA-20 场景 1/2）
- [ ] 2.2 手工：编辑页不可达 URL 预览回退占位图，可达 URL 正常显示（WA-20 场景 3）
- [x] 2.3 门禁：`cd frontend-web && npm run build` 零报错
- [x] 2.4 回归：`node tests/e2e/ui-e2e-web.mjs` WC-03 等用例通过

## 3. 收尾

- [ ] 3.1 `openspec status --change 202610-管理端商品图片回退` 校验通过
- [ ] 3.2 人工确认后归档：delta 同步进 `openspec/specs/管理端/spec.md`，change 移入 `archive/`（禁止 AI 自动提交）
