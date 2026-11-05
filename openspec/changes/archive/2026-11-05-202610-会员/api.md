## 通用约定

- 响应包装 `{ code, data, msg }`，code=200 成功、401 未登录；分页 `page`(1 起)/`size`(默认 10)，分页数据 `{total, rows}` 放在 data 内。
- 登录态经请求头 `X-Token` 传递（买家前缀 MBR-，管理员前缀 ADM-，7 天有效）；金额一律整数「分」，时间 `yyyy-MM-dd HH:mm:ss`。
- 本 change 为「会员」能力基线，无接口变更；以下为当前实现的前后端联调接口快照（与「接口契约」主规格 AC-01/AC-05/AC-15 一致）。

## 接口总览

### 买家端（H5 消费）

| HTTP | URL | 说明 | 契约 |
|---|---|---|---|
| POST | /api/member/login | body `{phone, smsCode}`；验证码固定 123456，新手机号自动注册 → `data:{token, memberId, phone, levelName, points}` | AC-01 |
| GET | /api/member/me | → `{id, phone(脱敏), levelCode, levelName, points, totalPaid(分), nextLevelName, nextLevelGap(分)}` | AC-01 |
| GET | /api/member/points/logs?page=&size= | → `{total, rows:[{id, change(±), balanceAfter, reason, relatedNo, createdAt}]}` | AC-01 |
| GET | /api/addresses | → `[{id, receiver, phone, region, detail, isDefault}]` | AC-05 |
| POST | /api/addresses | body `{receiver, phone, region, detail, isDefault}`，上限 20 条 | AC-05 |
| PUT/DELETE | /api/addresses/{id} | 编辑/删除（仅本人地址） | AC-05 |
| POST | /api/addresses/{id}/default | 设默认（默认地址唯一） | AC-05 |

### 管理端（frontend-web 会员页消费）

| HTTP | URL | 说明 | 契约 |
|---|---|---|---|
| GET | /admin-api/members?keyword=&page=&size= | 会员列表，手机号脱敏 | AC-15 |
| POST | /admin-api/members/{id}/points | body `{change(±), reason(必填)}` 人工调整积分，留审计 | AC-15 |
| GET/PUT | /admin-api/level-rules | 等级阈值查询/批量更新，留审计 | AC-15 |

## 说明

- 会员等级由累计实付驱动（V0~V3，只升不降）；积分流水不可删改，退款按比例扣回。
- 变更记录：2026-10-23 基线快照，无接口变更；影响下游：H5 登录/会员中心/地址页、Web 会员管理页。
