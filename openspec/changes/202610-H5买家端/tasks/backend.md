## 1. 说明

- 「H5买家端」能力为 frontend-h5 前端，无后端改动；买家端接口归属各能力域（会员/商品/购物车/订单/售后/门店），见各 change 的 tasks 与 api.md。

## 2. 收尾门禁

- [x] 2.1 后端无改动，`cd backend && ./mvnw -q compile` 零报错（回归确认）
- [x] 2.2 `node tests/e2e/e2e-mall.mjs` 全部用例 PASS
