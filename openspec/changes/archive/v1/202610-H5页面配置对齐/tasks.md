# Tasks

## 1. 页面配置单一来源
- [x] 1.1 新增 `frontend-h5/src/pages.json`（window/pages/tabBar 三段式，对齐参考工程 app.config.ts 组织方式）（H5-08）
- [x] 1.2 重构 `src/router/index.js`：路由/标题/登录拦截元信息全部由 pages.json 生成（H5-08）
- [x] 1.3 重构 `src/components/TabBar.vue`：导航项与主题色来自 tabBar 配置，购物车角标声明式（H5-01/H5-08）

## 2. 质量门禁与归档
- [x] 2.1 `npm run build` 通过；dev 运行截图验证首页/详情/TabBar 渲染正常
- [x] 2.2 同步 h5-client 主规格（新增 H5-08），归档 change
