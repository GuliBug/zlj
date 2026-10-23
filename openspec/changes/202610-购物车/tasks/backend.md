## 1. 购物车接口（CartController/CartService，需会员登录）

- [x] 1.1 GET /api/cart：rows（含实时 stock、invalid 失效标识、checked）+ totalQuantity 角标数据源
- [x] 1.2 POST /api/cart 加购：合并同 SKU，单 SKU 上限 999、种类上限 100 报「购物车已满」
- [x] 1.3 POST /api/cart/update（数量/勾选）、/delete（批量）、/check-all（全选）、/clear-invalid（清除失效）

## 2. 收尾门禁

- [x] 2.1 `cd backend && ./mvnw -q compile` 零报错
- [x] 2.2 `node tests/e2e/e2e-mall.mjs` 全部用例 PASS，docs/测试用例.md 与 docs/测试报告.md 已更新
