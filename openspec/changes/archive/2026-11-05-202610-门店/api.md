## 通用约定

- 响应包装 `{ code, data, msg }`，code=200 成功；买家门店接口无需登录；管理端门店与核销接口需管理员登录。
- 本 change 为「门店」能力基线（单店模式），无接口变更；以下为当前实现的前后端联调接口快照（与「接口契约」主规格 AC-02/AC-12 一致）。

## 接口总览

### 买家端（H5 消费）

| HTTP | URL | 说明 | 契约 |
|---|---|---|---|
| GET | /api/stores | → `[{id, name, address, phone, operatingHours, status(OPEN\|CLOSED)}]`；单店模式返回单元素（正隆记总号） | AC-02 |

### 管理端（frontend-web 门店页消费）

| HTTP | URL | 说明 | 契约 |
|---|---|---|---|
| GET | /admin-api/stores | 门店列表 | AC-12 |
| POST | /admin-api/stores | body `{name, address, phone, operatingHours, status}`；契约保留，单店模式前端无新增入口 | AC-12 |
| PUT | /admin-api/stores/{id} | 编辑门店信息，保存即时生效 | AC-12 |
| GET | /admin-api/verify-records?page=&size=&storeId= | 核销记录 `{id, orderNo, storeName, verifier, verifyTime, payAmount}` | AC-12 |
| POST | /admin-api/verify | body `{orderNo, code, storeId}`；校验 PICKUP_WAIT + 当前/上一分钟窗口码 + 门店匹配 → COMPLETED + 实扣 + 留痕 | AC-12 |

## 说明

- 单店模式口径：种子数据仅默认门店；POST /admin-api/stores 契约保留（未来开分店恢复），前端不提供入口。
- 变更记录：2026-10-23 基线快照，无接口变更；影响下游：H5 门店页/结算自提/核销码页、Web 门店管理与核销页。
