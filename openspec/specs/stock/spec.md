# stock Specification

## Purpose
库存三维口径管理：实物=可用+预占+冻结；入库单（批次必填）、冻结/释放、阈值预警、流水追溯（不可删改）、盘点单二级审批、日终核对、预占超时释放兜底，覆盖 FR-11。

## Requirements

### Requirement: FR-11-1 三维口径
所有库存列表 SHALL 同时展示实物/可用/预占/冻结四列，且 恒等式 实物=可用+预占+冻结 成立。

### Requirement: FR-11-2 入库单
入库 SHALL 以入库单为单位：SKU、数量>0、批次号必填、供应商/备注选填；确认后生成 IN 流水。

#### Scenario: 批次号必填
- **WHEN** 入库不填批次号
- **THEN** 系统拒绝

### Requirement: FR-11-3 盘点二级审批
盘点 SHALL 以盘点单为单位：实盘数→差异自动计算→管理员二级确认（提交人≠确认人）→确认后按差异修正库存并生成 ADJUST 流水。

#### Scenario: 提交人自确认被拒
- **WHEN** 提交人本人确认盘点单
- **THEN** 系统拒绝

### Requirement: FR-11-4 冻结与释放
SHALL 支持 SKU 级冻结/释放：冻结后可用清零（买家端立即售罄置灰），预占中订单不受影响；均生成流水。

#### Scenario: 冻结后不可购
- **WHEN** SKU 被冻结后买家下单
- **THEN** 下单失败提示缺货

### Requirement: FR-11-5 扣减链路与兜底
库存链路 SHALL 为：下单预占→支付实扣→取消/超时释放；预占超时由关单任务兜底释放（15 分钟）。

#### Scenario: 支付实扣
- **WHEN** 订单支付成功
- **THEN** 预占转实扣（实物与预占减少），生成 OUT 流水

### Requirement: FR-11-6 阈值预警
安全阈值每 SKU 可配（默认 50，留审计）；可用低于阈值触发告警（STOCK_ALERT），同 SKU 24 小时内不重复推送。

### Requirement: FR-11-7 流水可追溯
库存流水 SHALL 完整记录来源（IN/OUT/RESERVE/RELEASE/REFUND_IN/ADJUST/FREEZE/UNFREEZE）与关联单号，不可删除、不可修改；日终核对实物=Σ流水，差异触发 RECON_DIFF 告警。

#### Scenario: 流水与实物一致
- **WHEN** 执行日终核对
- **THEN** 每 SKU 实物库存等于全部流水净和，差异登记告警
