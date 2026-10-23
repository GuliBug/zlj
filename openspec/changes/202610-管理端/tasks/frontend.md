## 1. 工程与布局（frontend-web）

- [x] 1.1 依赖与脚本对齐参考工程（Element Plus/Pinia/vue-router/axios 等），侧栏 7 菜单布局与主题（主色 #8f1d22、侧栏 #241d18）
- [x] 1.2 utils/request.js：X-Token 注入、`{code,data,msg}` 解包、401 清 token 跳登录、错误 ElMessage；utils/format.js `fen2yuan` 金额统一
- [x] 1.3 路由守卫：未登录跳登录页（携 redirect）；views/login/index.vue 账密登录（回车提交、loading、演示账号提示）
- [x] 1.4 layout/index.vue：侧栏/顶栏/内容区，审计与告警自工作台资损监控入口进入

## 2. 页面

- [x] 2.1 dashboard/index.vue：GMV/订单数/支付成功率/客单价等指标卡、今日概览、近 7 日趋势
- [x] 2.2 audit/index.vue：审计日志（模块筛选、操作人/时间/IP/前后值）与告警列表（五类）
- [x] 2.3 危险操作二次确认覆盖（上下架/退款/积分调整双重确认/盘点确认/删除地址）

## 3. 收尾门禁

- [x] 3.1 `cd frontend-web && npm run build` 零报错
- [x] 3.2 `node tests/e2e/ui-e2e-web.mjs` 全部用例 PASS（登录/七菜单页/违禁词拦截/无页面 JS 异常），结果并入 docs/测试报告.md
