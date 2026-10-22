### Requirement: H5-09 吸底操作栏与 TabBar 叠放
当页面同时存在底部 TabBar 与吸底操作栏（`.bottom-bar`）时，吸底操作栏 SHALL 抬升至 TabBar 之上（`bottom: 52px`），页面底部留白 SHALL 同步预留两层高度；任何关键操作按钮（结算/提交订单/支付）SHALL 在 375×667 与 390×844 视口下中心点不被其他元素遮挡（`elementFromPoint` 校验）。

#### Scenario: 购物车结算
- **WHEN** 购物车存在商品并进入购物车页
- **THEN** 底部「结算(N)」按钮完整可见、可点击，位于 TabBar 上方，点击后进入确认订单页

#### Scenario: UI 级回归
- **WHEN** 运行 `node tests/e2e/ui-e2e.mjs`（需后端与 H5 dev 已启动）
- **THEN** UC-01~UC-06 全部 PASS，任一元素被遮挡即 FAIL

### Requirement: H5-10 前端工具链对齐
H5 工程依赖与脚本 SHALL 对齐参考工程工具链（sass / eslint9 / eslint-plugin-vue / prettier 及版本约束）；因本工程仅交付 H5 端（参考工程 mall-client 为 Taro4 weapp+h5 双端），不引入 `@tarojs/*`，该偏差已在 changes 记录为有意架构决策。
