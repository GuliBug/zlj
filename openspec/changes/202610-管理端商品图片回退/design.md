# Design: 202610-管理端商品图片回退

## 根因回顾

演示数据商品图路径 `/img/zhenglongji-*.jpg` 无任何服务托管（后端 resources 无静态图、两端 Vite 均未代理 `/img`）→ `el-image` 请求 404 → 无 error 兜底显示「加载失败」。H5 端 `GoodsMedia` 监听 `@error` 回退占位块，故无此问题。

## 方案（方案 A，用户已确认）

1. **共享占位常量**：新增 `frontend-web/src/utils/placeholder.js`，导出 `PLACEHOLDER_IMG`（即现商品列表页内联的红金品牌 SVG data-URI），消除两处重复。
2. **商品列表**（`views/product/index.vue`）：`el-image` 增加 `#error` 插槽渲染 `<img :src="PLACEHOLDER_IMG">`；`preview-src-list` 同步兜底（404 时预览用占位图）。`row.image` 为空时行为不变（直接用占位图）。
3. **商品编辑页**（`views/product/edit.vue`）：主图与详情图预览的 `el-image` 增加 `#error` 插槽，回退同一占位图；不改动表单数据（`imagesText` 原样保留）。

## 备选与取舍

- **备选 B**：后端托管 `/img/*` 占位图资源。拒绝：无真实商品图资产，生成的占位图与前端 SVG 观感一致，却引入后端静态资源维护成本；且未来接入真实图床/OSS 后需再次清理。
- **备选 C**：Vite 代理 `/img` 到后端。拒绝：后端同样没有该资源，仅转移 404 位置。

## 测试要点

- 手工：商品列表所有商品缩略图均显示（原图或品牌占位），无「加载失败」。
- 手工：编辑页填入不可达 URL（如 `/img/not-exist.jpg`）→ 预览显示占位图；填入可达 URL → 显示原图。
- 门禁：`cd frontend-web && npm run build` 零报错；`node tests/e2e/ui-e2e-web.mjs` 回归 WC-03（商品列表渲染）通过。
