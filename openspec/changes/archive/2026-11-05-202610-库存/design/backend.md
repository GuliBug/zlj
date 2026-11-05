## Context

库存域实现于 `StockService` 与 `AdminStockController`（SKU 四维列表/入库/冻结/阈值/流水/盘点），`StockReconcileTask` 日终核对，库存流水 `StockFlow` 为唯一事实来源。

## Goals / Non-Goals

**Goals:**

- 记录库存域基线设计口径（四维恒等式、流水单一事实、盘点二级审批、告警去重）。

**Non-Goals:**

- 不改动任何代码；不做多仓/批次深度管理（仅批次号登记）。

## Decisions

### BD1 四维口径恒等式

实物 = 可用 + 预占 + 冻结，所有列表接口同源返回四列；任何库存变动必须走流水（IN/OUT/RESERVE/RELEASE/REFUND_IN/ADJUST/FREEZE/UNFREEZE）并关联业务单号。

### BD2 日终核对兜底

`StockReconcileTask` 校验每 SKU 实物=Σ流水，差异登记 RECON_DIFF 告警；与订单超时关单任务（15 分钟释放预占）共同兜底一致性和预占泄漏。

### BD3 盘点二级审批

盘点单差异自动计算（PENDING_CONFIRM），确认接口校验提交人≠确认人（违反 code=500）；确认后按差异修正库存并生成 ADJUST 流水——防止自盘自改。

### BD4 阈值告警 24h 去重

可用跌破阈值触发 STOCK_ALERT，同 SKU 24 小时内不重复推送；阈值修改留审计。

## Risks / Trade-offs

- [H2 演示库无真实日终] → 核对任务按固定周期执行即可验证口径；生产接 MySQL 后沿用同一任务。

## Open Questions

（无）
