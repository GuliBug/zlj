## 通用约定

- 响应包装 `{ code, data, msg }`，code=200 成功；金额一律整数「分」；违禁词命中 code=500 且 msg 列出命中词。
- 本 change 为「商品」能力基线，无接口变更；以下为当前实现的前后端联调接口快照（与「接口契约」主规格 AC-03/AC-10 一致）。

## 接口总览

### 买家端（H5 消费）

| HTTP | URL | 说明 | 契约 |
|---|---|---|---|
| GET | /api/categories | 分类树 → `[{id, name, parentId, children:[...]}]` | AC-03 |
| GET | /api/products?keyword=&categoryId=&page=&size= | 仅上架可见；keyword 匹配名称/条码/品牌 → `{total, rows:[{id, name, image, priceMin(分), priceMax, sales, status}]}` | AC-03 |
| GET | /api/products/{id} | 详情：brand/origin/description/images[]/categoryName/status/warnText(固定警示语)/minorText/skus[{id, flavor, spec, barcode, price(分), stock(可用), sales}] | AC-03 |
| POST | /api/products/{id}/remind | 到货提醒登记（需登录；在售商品拒绝）→ `data:"ok"` | AC-03 |

### 管理端（frontend-web 商品页消费）

| HTTP | URL | 说明 | 契约 |
|---|---|---|---|
| GET | /admin-api/products?keyword=&status=&categoryId=&page=&size= | 商品列表（含 skus 摘要），支持导出场景 | AC-10 |
| POST | /admin-api/products | 新增商品（违禁词校验、条码唯一、售价>0、库存≥0） | AC-10 |
| PUT | /admin-api/products/{id} | 编辑（改价对未支付订单不生效；留审计） | AC-10 |
| POST | /admin-api/products/status | body `{ids, status}` 批量上下架，即时生效，留审计 | AC-10 |
| POST | /admin-api/check-banned-words | body `{text}` → `{hits:[...]}` 实时违禁词校验 | AC-10 |
| GET/POST | /admin-api/categories | 分类查询/新增 `{name, parentId}` | AC-10 |

## 说明

- 合规硬编码：warnText「长期过量咀嚼槟榔，有害口腔健康」、minorText「不向未成年人销售」由后端字段固定返回。
- 变更记录：2026-10-23 基线快照，无接口变更；影响下游：H5 首页/分类/详情页、Web 商品列表/新增编辑页。
