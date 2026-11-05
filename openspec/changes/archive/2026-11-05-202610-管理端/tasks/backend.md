## 1. 说明

- 「管理端」能力为 Web 管理端前端（frontend-web），无后端改动；管理端接口归属各能力域（商品/库存/门店/订单/售后/会员/平台），见各 change 的 tasks 与 api.md。

## 2. 收尾门禁

- [x] 2.1 后端无改动，`cd backend && ./mvnw -q compile` 零报错（回归确认）
- [x] 2.2 `node tests/e2e/e2e-mall.mjs` 全部用例 PASS
