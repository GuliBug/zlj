## Context

平台横切层实现于 `framework/security`（LoginInterceptor/LoginHelper/@RequireLogin/TokenService）、`AuditService/AuditLog`、`AlertService/Alert`、`OrderTimeoutTask` 与 `StockReconcileTask`，管理端入口 `AdminLoginController/AdminPlatformController`。

## Goals / Non-Goals

**Goals:**

- 记录平台层基线设计口径（令牌、审计覆盖、告警分类、定时任务）。

**Non-Goals:**

- 不改动任何代码；不引入 RBAC/合规中心（v1.2 已移除勿恢复）。

## Decisions

### BD1 双前缀令牌 + 注解鉴权

X-Token 承载登录态（MBR-/ADM- 前缀，7 天），`@RequireLogin(LoginType.MEMBER|ADMIN)` 声明式校验，拦截器统一拦截——控制器零鉴权样板代码，越权类型天然隔离。

### BD2 审计覆盖资金与库存敏感操作

改价/上下架/积分调整/人工调分/盘点确认/阈值修改统一经 `AuditService.log(module, action, detail)` 落库（操作人/时间/IP/变更前后值），不可删改；查询按 module 过滤。

### BD3 告警五类入 alerts 表

DUPLICATE_PAY（重复支付）/ABNORMAL_AMOUNT（>5000 元）/STATUS_ROLLBACK（状态回跳）/RECON_DIFF（对账差异）/STOCK_ALERT（库存低于阈值，同 SKU 24h 去重）；告警只增不改，管理端分页可查。

### BD4 定时任务双兜底

`OrderTimeoutTask` 30s 扫描超 15 分钟待支付订单（关单前二次校验支付状态，关单释放预占）；`StockReconcileTask` 日终核对实物=Σ流水，差异登记 RECON_DIFF。

### BD5 管理端登录锁定

连续 5 次失败锁定 30 分钟（期间正确密码也拒绝），防暴力破解；演示种子 admin/admin123、manager/manager123。

## Risks / Trade-offs

- [H2 内存库重启即清空] → 审计/告警留存口径为「演示环境生命周期内不清理」；生产接 MySQL 后按 ≥5 年/≥2 年部署要求留存。

## Open Questions

（无）
