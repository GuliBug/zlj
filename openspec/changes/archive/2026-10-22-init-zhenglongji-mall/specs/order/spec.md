# order Specification

## Purpose
交易闭环核心：下单（幂等+库存预占+积分抵现+运费）、支付（模拟通道+双通道确认语义）、订单状态机（WAIT_PAY→PAID→SHIPPED→COMPLETED，含 PICKUP_WAIT 与 CLOSED/AFTER_SALE 分支）、超时关单、取消、确认收货、买家订单列表/详情、自提核销码，覆盖 FR-04、FR-05、FR-04-8。

## ADDED Requirements

### Requirement: FR-04-1 服务端计价
下单 SHALL 由服务端计算商品合计/运费/积分抵扣/应付，金额以「分」整数计算；页面展示值=服务端返回值。

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
支付结果 SHALL 以「回调+主动查询」双通道确认（本期模拟通道：POST pay 即成功，接口语义保留接入点）；重复支付被拒（幂等）。

#### Scenario: 重复支付被拒
- **WHEN** 已支付订单再次调用支付
- **THEN** 系统拒绝且资损监控登记 DUPLICATE_PAY 告警

### Requirement: FR-04-7 金额风控
单笔订单应付上限 SHALL 为 5000 元（500000 分），超限拦截。

### Requirement: FR-04-8 履约方式
SHALL 支持快递配送与到店自提（免运费、选门店）；自提订单支付后进入 PICKUP_WAIT，展示每分钟刷新的动态核销码（服务端分钟窗口哈希，校验当前+上一窗口）；买家可切换所属门店。

#### Scenario: 自提订单生成核销码
- **WHEN** 自提订单支付成功
- **THEN** 订单进入 PICKUP_WAIT 并可获取 6 位动态核销码

### Requirement: O-04 订单状态机
状态机 SHALL 单向流转：WAIT_PAY→(支付)PAID→(发货)SHIPPED→(确认/签收)COMPLETED；WAIT_PAY→(取消/超时)CLOSED；自提：PAID→PICKUP_WAIT→COMPLETED；COMPLETED↔AFTER_SALE；非法流转抛异常禁止回跳。

#### Scenario: 非法流转被拦
- **WHEN** 对 CLOSED 订单调用支付
- **THEN** 系统拒绝（禁止回跳）

### Requirement: FR-05-1 订单列表
买家订单 SHALL 按状态筛选（WAIT_PAY/PAID/SHIPPED/COMPLETED/AFTER_SALE 等）并按时间倒序分页。

### Requirement: FR-05-2 取消与确认收货
WAIT_PAY 可取消（释放库存）；SHIPPED 可确认收货，前端二次确认。

#### Scenario: 确认收货
- **WHEN** 买家对 SHIPPED 订单确认收货
- **THEN** 订单转 COMPLETED，可发起售后

### Requirement: FR-05-3 订单详情
详情 SHALL 展示订单号、明细、金额明细、支付方式、收货信息/自提门店、物流单号、状态时间轴。
