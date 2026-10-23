# Tasks: 202610-购物车UI与依赖修复

## 1. P0 缺陷修复
- [x] 1.1 修复购物车结算栏被 TabBar 遮挡（styles.css：`.has-tabbar .bottom-bar { bottom:52px }`，底部留白 122px）
- [x] 1.2 截图回归验证（docs/screenshots/h5-cart-fixed.png）

## 2. UI 级端到端测试
- [x] 2.1 新增 tests/e2e/ui-e2e.mjs（puppeteer + Edge，hash 路由适配）
- [x] 2.2 用例 UC-01~UC-06：登录/加购(SKU弹层)/购物车结算可见可点/确认订单提交/支付跳转/详情吸底栏
- [x] 2.3 全部通过并产出 ui-last-run.json

## 3. 依赖对齐
- [x] 3.1 frontend-web/package.json 对齐 qingguoji-mall-web（+js-cookie/nprogress/sass/eslint9/prettier，scripts 命名一致）
- [x] 3.2 frontend-h5/package.json 工具链对齐（+sass/eslint9/prettier；保留纯 Vue3 不引入 Taro，偏差已记录）
- [x] 3.3 npm install + build:prod 双端构建通过
- [x] 3.4 dev server 重启验证

## 4. 文档
- [x] 4.1 更新 docs/测试用例.md、docs/测试报告.md（新增 UI 级用例章节）
- [x] 4.2 同步主规格库 h5-client / web-admin / platform
