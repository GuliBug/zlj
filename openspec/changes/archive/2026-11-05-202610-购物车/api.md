## 通用约定

- 响应包装 `{ code, data, msg }`，code=200 成功；金额一律整数「分」；购物车接口整体需会员登录（X-Token，MBR- 前缀）。
- 本 change 为「购物车」能力基线，无接口变更；以下为当前实现的前后端联调接口快照（与「接口契约」主规格 AC-04 一致）。

## 接口总览

| HTTP | URL | 说明 | 契约 |
|---|---|---|---|
| GET | /api/cart | → `{rows:[{id, skuId, productId, name, image, flavor, spec, price(分), quantity, stock(实时), checked, invalid}], totalQuantity(角标数据源)}` | AC-04 |
| POST | /api/cart | body `{skuId, quantity}`；合并同 SKU，单 SKU 上限 999，种类上限 100 报「购物车已满」 | AC-04 |
| POST | /api/cart/update | body `{id, quantity?, checked?}` | AC-04 |
| POST | /api/cart/delete | body `{ids:[...]}` | AC-04 |
| POST | /api/cart/check-all | body `{checked}` 全选/全不选 | AC-04 |
| POST | /api/cart/clear-invalid | 一键清除失效行 | AC-04 |

## 说明

- `invalid=true` 表示 SKU 已下架/失效，行不可勾选结算；`stock` 为实时可用库存，H5 据此展示「库存不足，仅剩 N 件」并限制步进上限。
- 变更记录：2026-10-23 基线快照，无接口变更；影响下游：H5 购物车页、商品详情加购入口、TabBar 角标。
