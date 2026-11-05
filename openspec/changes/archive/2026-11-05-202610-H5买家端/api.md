## 通用约定

- 响应包装 `{ code, data, msg }`，code=200 成功、401 未登录；金额一律整数「分」。
- 本 change 为「H5买家端」能力基线（纯前端能力），无接口变更；本文件汇总 H5 各页面消费的买家端接口（字段级契约见各能力域 change 的 api.md 与主规格 AC-01~AC-08）。

## 接口总览（H5 页面 → 接口消费映射）

| 页面 | 消费接口 | 契约 |
|---|---|---|
| 登录 / 验证码 | POST /api/member/login | AC-01 |
| 首页 / 全部商品 / 详情 | GET /api/categories、/api/products、/api/products/{id}、POST /api/products/{id}/remind | AC-03 |
| 购物车 / 详情角标 | GET/POST /api/cart、/api/cart/update、/delete、/check-all、/clear-invalid | AC-04 |
| 确认订单 | GET /api/cart（勾选行）、/api/stores、/api/addresses、/api/member/me；POST /api/orders | AC-04/02/05/01/06 |
| 支付结果 | POST /api/orders/{no}/pay、GET /api/orders/{no} | AC-06 |
| 订单列表 / 详情 | GET /api/orders、/api/orders/{no}、POST pay/cancel/confirm | AC-06 |
| 核销码（页面与订单详情） | GET /api/orders/{no}/verify-code | AC-07 |
| 售后申请 / 列表 / 详情 | GET /api/aftersales/reasons、POST /api/aftersales、GET 列表/详情、POST cancel | AC-08 |
| 地址列表 / 编辑 | GET/POST/PUT/DELETE /api/addresses、POST /{id}/default | AC-05 |
| 会员中心 | GET /api/member/me、/api/member/points/logs、/api/addresses、/api/orders | AC-01/05/06 |
| 门店信息 | GET /api/stores | AC-02 |

## 说明

- 401 由 api.js 全局拦截清除 token 并跳转登录页（携 redirect 回跳）；购物车角标经 totalQuantity 全局刷新。
- 变更记录：2026-10-23 基线快照，无接口变更；影响下游：frontend-h5 全部页面与 TabBar。
- 变更记录：2026-10-26 修订：仍无接口行为变更；补记 `GET /api/products/{id}` 响应字段 `detailImages[]`（管理端商品编辑已支持详情图，后端实际返回、详情页消费），主规格 AC-03 同步补记。
