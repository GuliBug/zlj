## 1. H5（frontend-h5）

- [x] 1.1 Cart.vue：失效商品分区（一键清除）、有效行勾选/步进（上限贴实时库存）/删除二次确认、吸底合计栏（勾选行汇总）
- [x] 1.2 空态「去首页」引导；未勾选结算提示拦截
- [x] 1.3 store.js refreshCartCount：加购/删除/改数后全局刷新 TabBar 角标（99+ 封顶）
- [x] 1.4 收尾：`cd frontend-h5 && npm run build` 零报错；`node tests/e2e/ui-e2e.mjs` 全 PASS（购物车结算按钮可见可点断言）
