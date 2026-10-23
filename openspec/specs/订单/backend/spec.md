# 订单 Specification — backend

## Purpose
交易闭环系统侧：下单（幂等+库存预占+积分抵现+运费）、支付（模拟通道+双通道确认语义）、订单状态机（WAIT_PAY→PAID→SHIPPED→COMPLETED，含 PICKUP_WAIT 与 CLOSED 分支）、超时关单、取消、确认收货、列表查询，覆盖 FR-04、FR-05 的系统行为与数据口径。

## Requirements

### Requirement: FR-04-1 服务端计价
下单 SHALL 由服务端计算商品合计/运费/积分抵扣/应付，金额以「分」整数计算；运费 EXPRESS 满 9900 分包邮否则 1000 分，PICKUP 恒为 0；积分抵现 100 积分=1 元且 ≤ 抵扣前金额 5%。

#### Scenario: 运费规则
- **WHEN** EXPRESS 订单商品合计满 99 元
- **THEN** 运费为 0，否则 10 元；PICKUP 订单运费恒为 0

### Requirement: FR-04-2 下单幂等
提交订单 SHALL 携带客户端 UUID 幂等键，服务端 5 分钟内去重，重复点击不产生重复订单。

#### Scenario: 重复提交返回原单
- **WHEN** 同幂等键 5 分钟内二次提交
- **THEN** 返回首次订单号，订单总数不变

### Requirement: FR-04-3 库存预占
下单 SHALL 与库存预占同一事务（乐观锁条件更新）；库存不足返回明确缺货 SKU 与剩余可购数量，整单回滚。

#### Scenario: 并发下单不超卖
- **WHEN** 可用库存 1 时两个请求同时下单
- **THEN** 仅一个成功，另一个收到缺货提示

### Requirement: FR-04-5 超时关单
待支付订单超 15 分钟 SHALL 自动关闭并释放预占库存，关单前二次校验支付状态。

#### Scenario: 超时释放库存
- **WHEN** 待支付订单超 15 分钟被定时任务扫描
- **THEN** 订单置 CLOSED，可用库存回补

### Requirement: FR-04-6 支付结果确认
支付结果 SHALL 以「回调+主动查询」双通道确认（本期模拟通道：POST pay 即成功，接口语义保留接入点）；重复支付被拒并触发资损告警。

#### Scenario: 重复支付被拒
- **WHEN** 已支付订单再次调用支付
- **THEN** 系统拒绝且资损监控登记 DUPLICATE_PAY 告警

### Requirement: FR-04-7 金额风控
单笔订单应付上限 SHALL 为 5000 元（500000 分），超限拦截。

#### Scenario: 超限拦截
- **WHEN** 订单应付金额计算结果超过 500000 分
- **THEN** 下单被拒绝并提示金额超限

### Requirement: FR-04-8 履约方式
SHALL 支持快递配送与到店自提（免运费、选门店）；自提订单支付后进入 PICKUP_WAIT，展示每分钟刷新的动态核销码（服务端分钟窗口哈希，核销校验接受当前+上一窗口码）。

#### Scenario: 自提订单生成核销码
- **WHEN** 自提订单支付成功
- **THEN** 订单进入 PICKUP_WAIT 并可获取 6 位动态核销码

### Requirement: O-04 订单状态机
状态机 SHALL 单向流转：WAIT_PAY→(支付)PAID→(发货)SHIPPED→(确认/签收)COMPLETED；WAIT_PAY→(取消/超时)CLOSED；自提：PAID→PICKUP_WAIT→COMPLETED；COMPLETED↔AFTER_SALE；非法流转抛异常禁止回跳。

#### Scenario: 非法流转被拦
- **WHEN** 对 CLOSED 订单调用支付
- **THEN** 系统拒绝（禁止回跳）

### Requirement: FR-05-1 订单列表
买家订单 SHALL 按状态筛选（WAIT_PAY/PAID/SHIPPED/PICKUP_WAIT/COMPLETED/CLOSED/AFTER_SALE）并按时间倒序分页；行内含商品摘要 items。

#### Scenario: 状态筛选与倒序
- **WHEN** 买家按状态 WAIT_PAY 筛选订单列表
- **THEN** 仅返回该状态订单且按下单时间倒序分页

### Requirement: FR-05-2 取消与确认收货
买家 SHALL 可取消 WAIT_PAY 订单（释放预占库存）；SHIPPED 订单 SHALL 支持确认收货，前端二次确认。

#### Scenario: 确认收货
- **WHEN** 买家对 SHIPPED 订单确认收货
- **THEN** 订单转 COMPLETED，可发起售后

### Requirement: O-05 订单详情返回商品明细
订单详情接口（GET /api/orders/{no} 与管理端详情）SHALL 返回 `items` 数组，每项含 `skuId/name/image/flavor/spec/price/quantity/subtotal`；详情含金额明细、收货信息/自提门店、物流信息、状态时间轴；存在售后单时时间轴合并最新售后流转记录（action 前缀「售后 · 」）并返回 `aftersale` 摘要。

#### Scenario: 详情返回商品明细
- **WHEN** 买家或管理端查询订单详情
- **THEN** items 数组逐项返回 skuId/name/image/flavor/spec/price/quantity/subtotal

#### Scenario: 售后进度合并展示
- **WHEN** 订单存在进行中的售后单且买家查询详情
- **THEN** 时间轴含「售后 · 」前缀节点，aftersale 摘要返回售后单号/状态/类型/退款金额
