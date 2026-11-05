## 通用约定

- 响应包装 `{ code, data, msg }`，code=200 成功；售后接口整体需会员登录；管理端售后接口需管理员登录（ADM- 前缀）。
- 本 change 为「售后」能力基线，无接口变更；以下为当前实现的前后端联调接口快照（与「接口契约」主规格 AC-08/AC-14 一致）。

## 接口总览

### 买家端（H5 消费）

| HTTP | URL | 说明 | 契约 |
|---|---|---|---|
| GET | /api/aftersales/reasons | → 原因枚举（质量问题/少件漏发/发错货/不想要了/其他） | AC-08 |
| POST | /api/aftersales | body `{orderNo, type(REFUND_ONLY\|RETURN_REFUND), reason, description?, images?[]}` → `{afterSaleNo}`；仅 COMPLETED/SHIPPED 且签收 7 天内；每订单每 SKU 限 3 次 | AC-08 |
| GET | /api/aftersales?page=&size= | → `{total, rows:[{afterSaleNo, orderNo, type, typeText, status, statusText, refundAmount, createdAt}]}` | AC-08 |
| GET | /api/aftersales/{no} | 详情含 `flow:[{action, operator, remark, time}]` 时间线（只读不可删改） | AC-08 |
| POST | /api/aftersales/{no}/cancel | 仅商家审核前可撤销 | AC-08 |
| POST | /api/aftersales/{no}/return-logistics | body `{company, no}` 退货退款态填寄回物流 | AC-08 |

### 管理端（frontend-web 售后页消费）

| HTTP | URL | 说明 | 契约 |
|---|---|---|---|
| GET | /admin-api/aftersales?status=&page=&size=&overdue= | 超 48h 未审核行 `auditOverdue:true` | AC-14 |
| GET | /admin-api/aftersales/{no} | 详情含买家申请信息 + flow 时间线 | AC-14 |
| POST | /admin-api/aftersales/{no}/audit | body `{approve, opinion(必填)}`；通过：仅退款直接退款，退货退款 → WAIT_RETURN | AC-14 |
| POST | /admin-api/aftersales/{no}/confirm-return | 退货退款确认收货 → 退款 + 库存回补 | AC-14 |
| POST | /admin-api/aftersales/{no}/refund | 幂等退款；成功后 → REFUNDED，积分按比例扣减 | AC-14 |

## 说明

- 退款金额服务端计算，前端只读展示；退款幂等（重复调用不重复退款）。
- 变更记录：2026-10-23 基线快照，无接口变更；影响下游：H5 售后申请/列表/详情、Web 售后审核页。
