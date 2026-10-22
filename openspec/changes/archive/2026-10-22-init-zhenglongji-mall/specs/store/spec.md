# store Specification

## Purpose
门店管理与到店自提核销：门店信息维护、买家门店切换、核销码校验（状态/门店/时效）、核销留痕，覆盖 FR-04-8、FR-12 门店部分。

## ADDED Requirements

### Requirement: ST-01 门店维护
管理端 SHALL 支持门店增改与启停（OPEN/CLOSED）；H5 提供门店列表与切换（顶部切换器），商品与核销跟随门店。

### Requirement: ST-02 核销校验
核销 SHALL 校验：订单状态=PICKUP_WAIT、门店与订单匹配、核销码为当前或上一分钟窗口有效码；任一不满足返回明确失败原因。

#### Scenario: 门店不匹配拒绝
- **WHEN** A 店核销 B 店自提订单
- **THEN** 核销失败提示门店不匹配

### Requirement: ST-03 核销留痕
核销成功 SHALL：订单→COMPLETED、库存实扣、生成核销记录（店员/时间/订单/门店）供后台查询，订单时间轴追加节点。

#### Scenario: 核销完成闭环
- **WHEN** 店员扫码核销成功
- **THEN** 订单完成并可在后台核销记录中查到该笔
