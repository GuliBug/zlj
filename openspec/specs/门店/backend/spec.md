# 门店 Specification — backend

## Purpose
门店管理与到店自提核销系统侧：门店信息维护（单店模式）、核销码校验（状态/门店/时效）、核销留痕、营业时间，覆盖 FR-04-8、FR-12 门店部分的系统行为与数据口径。

## Requirements

### Requirement: ST-01 门店维护

系统 SHALL 以单店模式运行：种子数据仅含默认门店「正隆记总号」，H5 展示该门店且不支持切换；管理端仅支持默认门店信息维护（编辑），不提供新增门店入口。

#### Scenario: 种子单店

- **WHEN** 演示环境初始化完成
- **THEN** 门店数据仅正隆记总号一家，买家端与管理端门店列表均为单元素

#### Scenario: 管理端无新增入口

- **WHEN** 管理员进入 Web 门店页
- **THEN** 无「新增门店」按钮，仅可编辑默认门店信息

#### Scenario: 门店启停

- **WHEN** 管理端将默认门店置为 CLOSED
- **THEN** 买家端门店列表状态同步为停用

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

### Requirement: ST-04 门店营业时间
门店实体 SHALL 含 operatingHours（营业时间）字段；门店列表与详情返回该字段；管理端门店编辑支持维护。

#### Scenario: 营业时间展示
- **WHEN** 买家端查看门店列表
- **THEN** 返回并展示 operatingHours 字段
