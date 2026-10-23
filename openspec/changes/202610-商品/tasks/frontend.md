## 1. H5（frontend-h5）

- [x] 1.1 Home.vue：品牌导航/健康警示条/hero/四宫格/分档 chips（分类接口动态渲染点击过滤）/双列网格（价格区间、已售万级缩写）/加载更多
- [x] 1.2 Category.vue：按顶级分类分节渲染网格，空分类展示空态
- [x] 1.3 ProductDetail.vue：大图区（GoodsMedia 兜底）/四正本纪工艺块/健康警示框/SKU 弹层（缺货置灰、单 SKU 自动选中、数量受库存约束）/吸底栏（下架降级）/加购与立即购买
- [x] 1.4 收尾：`cd frontend-h5 && npm run build` 零报错

## 2. Web 管理端（frontend-web）

- [x] 2.1 views/product/index.vue：分类/状态/名称/条码组合筛选、分页、导出 CSV
- [x] 2.2 views/product/edit.vue：三步步骤条（基本信息→SKU 与价格→图片与详情）、口味×规格笛卡尔积生成与批量填充、违禁词实时校验命中阻断、库存货值列
- [x] 2.3 收尾：`cd frontend-web && npm run build` 零报错；`node tests/e2e/ui-e2e-web.mjs` 全 PASS（含新增商品违禁词实时拦截断言）
