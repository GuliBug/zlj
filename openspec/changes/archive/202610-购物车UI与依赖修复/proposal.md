# Proposal: 202610-购物车UI与依赖修复

## Why

用户实测反馈两个问题：

1. **购物车无法下单（P0 缺陷）**：购物车页吸底结算栏 `.bottom-bar` 与底部 TabBar 均为 `position: fixed; bottom: 0`，且 TabBar `z-index:70 > bottom-bar z-index:60`，两者叠加时结算按钮被 TabBar 完全遮挡，用户无法点击结算——H5 买家主链路在购物车页断裂。
2. **前端依赖未对齐参考工程**：frontend-web 缺少 `js-cookie`、`nprogress`、`sass` 及 eslint/prettier 工具链与参考工程一致的脚本命名（build:prod/lint/format）；frontend-h5 工具链同样未对齐。工程规范性不满足「参考 qingguoji-mall 工程约定」的要求。

同时暴露测试盲区：原 E2E 为纯接口级断言，无法发现「UI 元素被遮挡」类缺陷，需补充 UI 级端到端测试。

## What Changes

- **[FIX] h5-client**：新增 CSS 规则 `.has-tabbar .bottom-bar { bottom: 52px }`，购物车页吸底结算栏抬升至 TabBar 之上；页面底部留白同步加到 122px。
- **[TEST] tests**：新增 `tests/e2e/ui-e2e.mjs`——基于真实浏览器（puppeteer + Edge）的 UI 级端到端，6 条用例（UC-01~UC-06），核心断言为「关键操作元素可见且可点击（elementFromPoint 无遮挡）」，覆盖 登录→加购（SKU 弹层）→购物车结算→确认订单→支付→详情吸底栏 全链路。
- **[DEPS] web-admin**：package.json 对齐 qingguoji-mall-web——补 `js-cookie@3.0.5`、`nprogress@0.2.0`、`sass@1.83.0`、eslint9+prettier 全套，脚本命名对齐（build:prod/lint/format/preview:5174）。
- **[DEPS] h5-client**：工具链对齐——补 `sass`、eslint9+prettier；**保留纯 Vue3+Vite 不引入 Taro**（参考工程 mall-client 为 Taro4 weapp+h5 双端，本工程仅 H5 端，架构偏差已在 design 记录并经确认）。

## Impact

- Affected specs: `h5-client`（新增 UI 可用性 Requirement）、`web-admin`、`platform`（依赖对齐约定）
- Affected code: `frontend-h5/src/styles.css`、`frontend-h5/package.json`、`frontend-web/package.json`、`tests/e2e/ui-e2e.mjs`（新增）
- 无后端变更；无破坏性接口变更
