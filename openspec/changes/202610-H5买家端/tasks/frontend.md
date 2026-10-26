## 1. 工程与基建（frontend-h5）

- [x] 1.1 pages.json 页面配置单一来源（window 主题/pages 清单/tabBar 4 项：首页/全部商品/购物车/我的），路由与登录拦截由配置生成
- [x] 1.2 api.js：请求封装（`{code,data,msg}` 解包、X-Token 注入、401 全局拦截携 redirect 跳登录）；api.test.js 覆盖核心工具函数（vitest）
- [x] 1.3 store.js：购物车角标 refreshCartCount、applyDefaultStore 单店锁定、checkoutAddressId/buyNow 结算状态（buyNow 用后即清）
- [x] 1.4 公共组件：GoodsMedia（破图兜底渐变红底占位）、BrandNav（品牌印记/当前门店/四正条）、TabBar；ui.js（Toast/Confirm/Alert）
- [x] 1.5 视觉规范：红金国潮风、375px 基准、健康警示条与未成年人提示固定展示；吸底栏叠放 TabBar（bottom: 52px）

## 2. 页面（全部接真实接口，无原型示例数据）

- [x] 2.1 首页/全部商品/商品详情（SKU 弹层/合规文案/加购与立即购买）
- [x] 2.2 购物车/确认订单/支付结果
- [x] 2.3 订单列表/订单详情（时间轴/动态核销码）/门店核销码页
- [x] 2.4 售后申请/列表/详情；地址列表/编辑；会员中心；门店信息
- [x] 2.5 收尾：`cd frontend-h5 && npm run build` 零报错

## 3. 收尾门禁

- [x] 3.1 `npm test`（vitest）通过；`node tests/e2e/ui-e2e.mjs` 全部用例 PASS（含 elementFromPoint 按钮可见性断言），结果并入 docs/测试报告.md

## 4. 2026-10-26 修订：详情页多媒体与样式修复

- [x] 4.1 商品详情大图区多图轮播（原生 scroll-snap + 底部圆点指示），单图/破图回退 GoodsMedia 兜底
- [x] 4.2 商品详情新增「商品详情」区块渲染 detailImages（纵向铺满、懒加载，空字段不渲染）
- [x] 4.3 样式修复：大图区铺满容器（.d-img .g-media）、订单详情状态标签与标题间距（.card-title .tag）
- [x] 4.4 收尾：`cd frontend-h5 && npm run build` 零报错（2026-10-26 已验证）
- [x] 4.5 E2E 回归锚点：接口级 TC-H5-13（买家详情回传 detailImages[]）+ UI 级 UC-23（主图轮播）/UC-24（详情图区块）/UC-25（状态标签间距）；docs/测试用例.md 与 docs/测试报告.md 同步（149 接口级 + 48 UI 级全 PASS）
