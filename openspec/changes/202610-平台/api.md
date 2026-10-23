## 通用约定

- 响应包装 `{ code, data, msg }`，code=200 成功；管理端接口需管理员登录（X-Token，ADM- 前缀）。
- 本 change 为「平台」能力基线（后端横切能力），无接口变更；以下为平台层对外接口与行为口径（与主规格 AC-09/AC-16 一致）。

## 接口总览

| HTTP | URL | 说明 | 契约 |
|---|---|---|---|
| POST | /admin-api/login | 账密登录 → `{token(ADM-), username, nickname}`；连续 5 次失败锁定 30 分钟 | AC-09 |
| GET | /admin-api/audit-logs?page=&size=&module= | 审计日志（操作人/模块/动作/变更前后值/IP/时间），不可删改 | AC-16 |
| GET | /admin-api/alerts?page= | 告警列表：DUPLICATE_PAY/ABNORMAL_AMOUNT/STATUS_ROLLBACK/RECON_DIFF/STOCK_ALERT | AC-16 |

## 说明

- 登录态：X-Token 7 天有效，`@RequireLogin` + 拦截器按 MEMBER/ADMIN 类型校验；退出由前端清 token。
- 定时任务：订单超时关单（30s 扫描，15 分钟阈值）；日终库存核对（实物=Σ流水，差异告警）。
- 审计覆盖：改价/上下架/积分调整/积分人工调整/盘点确认/阈值修改等资金与库存敏感操作。
- 变更记录：2026-10-23 基线快照，无接口变更；影响下游：双端登录态、Web 审计与告警页。
