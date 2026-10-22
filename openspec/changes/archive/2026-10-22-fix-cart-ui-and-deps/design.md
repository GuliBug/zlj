# Design: 2026-10-22-fix-cart-ui-and-deps

## 1. 购物车结算栏遮挡（P0）

### 根因
- `.bottom-bar`（styles.css）：`position:fixed; bottom:0; z-index:60`
- `.tabbar`（TabBar.vue）：`position:fixed; bottom:0; z-index:70`
- 仅购物车页同时挂两个底栏（`class="has-tabbar has-bottom-bar"`），结算栏完全处于 TabBar 之下且被更高 z-index 覆盖。

### 方案取舍
| 方案 | 评价 |
|---|---|
| A. 结算栏抬升到 TabBar 之上（`bottom:52px`） | ✅ 保留双入口信息，符合电商惯例（淘宝/京东购物车同款布局） |
| B. 购物车页隐藏 TabBar | 丢失导航，改动语义大 |
| C. 合并结算栏与 TabBar | 结构重构，风险高 |

采用 A。TabBar 实际高度 = 图标26px + 文字15px + 上下padding 11px ≈ 52px。

## 2. UI 级 E2E（补测试盲区）

- 原接口级 E2E（e2e-mall.mjs，89 断言）验证业务规则正确性，但无法感知视觉遮挡。
- 新增 `ui-e2e.mjs`：puppeteer（复用 qingguoji node_modules，executablePath 指向本机 Edge）驱动真实页面；核心原语 `visuallyClickable(selector)` 用 `document.elementFromPoint(中心点)` 判定元素是否被遮挡——正是本次缺陷的检测器。
- 注意：H5 为 **hash 路由**，页面 URL 需用 `/#/path`。

## 3. 依赖对齐

- **frontend-web**：逐版本对齐 qingguoji-mall-web（vue 3.5.26 / element-plus 2.13.1 / axios 1.13.2 / vite 6.4.1 …），补 js-cookie、nprogress（本次仅装依赖不强制重构请求层，axios/nprogress 供后续演进）、sass、eslint9+prettier；scripts 命名对齐（`build:prod`、`preview --port 5174`、`lint`、`format`）。
- **frontend-h5**：参考工程 mall-client 为 Taro4 多端（`@tarojs/*` 全家桶 + 编译到 weapp/h5）。本工程产品范围只有 H5 端（PRD 无小程序诉求），引入 Taro 属纯增重无收益，**有意保留纯 Vue3+Vite 并记录为架构偏差**；对齐其工具链（sass/eslint/prettier 版本一致）。

## Risks

- npm install 期间 dev server 可能需重启（验证步骤包含）。
- elementFromPoint 判定受弹层/动画影响，用例间留 1.2s+ 缓冲。
