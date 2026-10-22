# Proposal: 对齐参考工程的 H5 页面配置（align-h5-page-config）

## Why

用户要求 H5 前端工程参考 qingguoji-mall（frontend-h5/mall-client/src/app.config.ts）的页面配置组织方式：`pages`（页面清单）+ `tabBar`（底部导航）+ `window`（主题窗口色）为单一声明式来源。当前 H5 路由与 TabBar 各自硬编码，页面清单分散，不利于增删页面对齐 PRD。

## What Changes

- **ADDED**（h5-client）：`H5-08 页面配置单一来源`——新增 `frontend-h5/src/pages.json`，包含 window 主题色（红底白字导航，#B01F24/#F7F2E9）、pages 页面清单（route/title/auth/tab）、tabBar 导航项（pagePath/text/icon/selectedColor）。
- `src/router/index.js` 改为从 pages.json 生成路由与登录拦截元信息（`import.meta.glob` 懒加载页面组件）。
- `src/components/TabBar.vue` 改为从 pages.json 的 tabBar.list 渲染，角标项由 `badge: "cart"` 声明。

## Impact

- 触达需求编号：H5-01（页面清单）新增配置约束 H5-08
- Affected specs：h5-client（新增 1 条需求）
- Out of Scope：不引入 Taro 框架（PRD 仅要求 H5 端，维持 Vue3 + Vite）；页面集合不变
