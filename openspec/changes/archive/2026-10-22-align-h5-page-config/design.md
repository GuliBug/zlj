# Design: align-h5-page-config

## 决策

- 采用与参考工程 app.config.ts 同构的 JSON 形态 `src/pages.json`（pages / tabBar / window 三段式），而非 Taro：PRD 明确仅 H5 端，无小程序编译目标，维持 Vue3 + Vite 运行时。
- 路由组件用 `import.meta.glob('../pages/*.vue')` 解析，pages.json 只写声明不写 import，增删页面零改动。
- tabBar 角标用 `badge: "cart"` 声明式挂载，TabBar 组件按配置渲染；选中色 selectedColor=#B01F24 与品牌红一致。
- 底部导航保留 5 项（首页/分类/购物车/订单/我的）：参考工程为 4 项（无分类），但 PRD FR-02-1 要求分类页独立入口，故在参考结构上扩展。

## 各方责任

- 仅前端 H5 改动，无接口/契约变化，无后端影响。

## 契约疑问

无。
