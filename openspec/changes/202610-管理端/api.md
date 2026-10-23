## 通用约定

- 响应包装 `{ code, data, msg }`，code=200 成功；管理端接口需管理员登录（X-Token，ADM- 前缀，连续 5 次失败锁定 30 分钟）。
- 本 change 为「管理端」能力基线（Web 管理端前端），无接口变更；登录/看板/审计为管理端自有接口，其余页面消费各能力域接口（商品 AC-10/库存 AC-11/门店 AC-12/订单 AC-13/售后 AC-14/会员 AC-15，见对应 change 的 api.md）。

## 接口总览

| HTTP | URL | 说明 | 契约 |
|---|---|---|---|
| POST | /admin-api/login | body `{username, password}` → `{token(ADM-), username, nickname}`（种子 admin/admin123、manager/manager123） | AC-09 |
| GET | /admin-api/dashboard | → `{gmvToday(分), orderCountToday, payRate, pendingShip, pendingAftersale, stockAlertCount, salesTrend:[{date, gmv, orderCount}](近7日)}` | AC-09 |
| GET | /admin-api/audit-logs?page=&size=&module= | → `{operator, module, action, detail(变更前后值), ip, createdAt}` | AC-16 |
| GET | /admin-api/alerts?page= | 告警：DUPLICATE_PAY/ABNORMAL_AMOUNT/STATUS_ROLLBACK/RECON_DIFF/STOCK_ALERT | AC-16 |

## 说明

- 退出登录由前端清除本地 token；token 过期（7 天）接口返回 401，前端跳转登录页。
- 变更记录：2026-10-23 基线快照，无接口变更；影响下游：Web 登录页/工作台/审计与告警入口。
