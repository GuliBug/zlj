# Proposal: fix-admin-product-image-fallback

## Why

Web 管理端商品列表中，除无图商品（走 `PLACEHOLDER_IMG`）外，其余商品缩略图全部显示「加载失败」。根因：演示数据（`DemoDataInitializer`）写入的商品图路径为 `/img/zhenglongji-*.jpg`，该静态资源在后端与前端均不存在（404）；`el-image` 无加载失败兜底。H5 端 `GoodsMedia` 有 `@error` 回退品牌占位块，故仅 Web 端观感受损。

## What Changes

- 仅改 `frontend-web`：
  1. 将商品列表页的 `PLACEHOLDER_IMG`（红金品牌 SVG 印章）提取为共享常量 `src/utils/placeholder.js`；
  2. 商品列表 `el-image` 增加 `#error` 插槽，图片 404 时回退到品牌占位图（与 H5 行为对齐）；
  3. 商品编辑页主图/详情图预览的 `el-image` 同样增加 `#error` 插槽。
- 不改后端、不新增图片资源；未来 `/img/*` 资源补齐后自动恢复正常加载，占位图仅在加载失败时出现。

## Impact

- 影响 spec：`web-admin`（ADDED 图片加载失败回退要求）。
- 影响代码：`frontend-web/src/utils/placeholder.js`（新增）、`views/product/index.vue`、`views/product/edit.vue`。
- 不涉及接口契约变更、不涉及 H5 端。
