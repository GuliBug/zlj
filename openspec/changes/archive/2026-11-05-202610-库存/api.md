## 通用约定

- 响应包装 `{ code, data, msg }`，code=200 成功；管理端库存接口需管理员登录（X-Token，ADM- 前缀）。
- 本 change 为「库存」能力基线，无接口变更；以下为当前实现的前后端联调接口快照（与「接口契约」主规格 AC-11 一致）。

## 接口总览

| HTTP | URL | 说明 | 契约 |
|---|---|---|---|
| GET | /admin-api/skus?keyword=&page=&size= | → `{total, rows:[{id, productName, flavor, spec, physical, available, reserved, frozen, threshold, status(NORMAL\|FROZEN)}]}` | AC-11 |
| POST | /admin-api/stock/inbounds | body `{skuId, quantity(>0), batchNo(必填), supplier?, remark?}` → 生成 IN 流水 | AC-11 |
| GET | /admin-api/stock/inbounds?page= | 入库单列表 | AC-11 |
| POST | /admin-api/stock/freeze | body `{skuId, action(FREEZE\|RELEASE), reason}`；冻结后可用清零、预占不受影响 | AC-11 |
| POST | /admin-api/stock/threshold | body `{skuId, threshold}`，留审计 | AC-11 |
| GET | /admin-api/stock/flows?skuId=&type=&page=&size= | 流水：type 枚举 IN/OUT/RESERVE/RELEASE/REFUND_IN/ADJUST/FREEZE/UNFREEZE，含 balanceAfter/relatedNo/remark | AC-11 |
| POST | /admin-api/stock/stocktakes | body `{items:[{skuId, actual}], remark}` → 盘点单（差异自动计算，PENDING_CONFIRM） | AC-11 |
| GET | /admin-api/stock/stocktakes?page= | → `{id, status(PENDING_CONFIRM\|CONFIRMED), items:[{skuId, productName, systemCount, actual, diff}], createdBy, confirmedBy}` | AC-11 |
| POST | /admin-api/stock/stocktakes/{id}/confirm | 二级确认（提交人≠确认人，违反 code=500）；确认后按差异修正并生成 ADJUST 流水 | AC-11 |

## 说明

- 恒等式 实物 = 可用 + 预占 + 冻结；扣减链路：下单预占 → 支付/核销实扣 → 取消/超时（15 分钟）释放。
- 流水不可删改；日终核对实物=Σ流水，差异触发 RECON_DIFF 告警；可用低于阈值触发 STOCK_ALERT（同 SKU 24h 不重复）。
- 变更记录：2026-10-23 基线快照，无接口变更；影响下游：Web 库存管理页（总览/流水/盘点三 Tab）。
