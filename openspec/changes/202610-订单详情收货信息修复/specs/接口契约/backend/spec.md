## MODIFIED Requirements

### Requirement: AC-06 订单接口 /api/orders

- `POST /api/orders` body `{idempotentKey(UUID), fulfillmentType(EXPRESS|PICKUP), storeId?(PICKUP必填), addressId?(EXPRESS必填), usePoints(bool), items:[{skuId, quantity}]}` → `data:{orderNo, goodsAmount(分), freight(分), pointsDeduction(分), payAmount(分), pointsUsed, status}`
  - 服务端计算全部金额；运费：EXPRESS 满 9900 分包邮否则 1000 分，PICKUP 为 0；积分抵现 100 积分=1 元且 ≤ payAmount 前 5%（抵扣前金额）；单笔 payAmount ≤ 500000 分
  - 幂等：同 idempotentKey 5 分钟内重复提交返回首次订单（data 相同），不重复创建
  - 库存不足：code=500，msg 含缺货 SKU 名称与剩余可购数量
- `GET /api/orders?status=&page=&size=` → `data:{total, rows:[{orderNo, status, statusText, payAmount, createdAt, items:[{name, image, flavor, spec, price, quantity}], fulfillmentType}]}`；status 枚举 WAIT_PAY/PAID/SHIPPED/PICKUP_WAIT/COMPLETED/CLOSED/AFTER_SALE
- `GET /api/orders/{no}` → 订单详情：含金额明细、收货信息/自提门店、物流信息 `{logisticsCompany, logisticsNo}`、时间轴 `timeline:[{action, time, remark}]`、PICKUP_WAIT 态含 `pickupCode`（服务端算当前分钟窗口码）
  - **收货信息（EXPRESS）为扁平字段**：`receiver`（收货人）、`receiverPhone`、`address`（含省市区与详细地址的完整字符串）；收货信息不是嵌套对象
- `POST /api/orders/{no}/pay` → 模拟支付成功；非 WAIT_PAY 拒绝（幂等）
- `POST /api/orders/{no}/cancel` → 仅 WAIT_PAY 可取消，释放预占
- `POST /api/orders/{no}/confirm` → 仅 SHIPPED 可确认收货 → COMPLETED

#### Scenario: 幂等下单
- **WHEN** 同 idempotentKey 5 分钟内重复提交
- **THEN** 返回首次订单，不产生重复订单

#### Scenario: 库存预占失败
- **WHEN** EXPRESS 下单时库存不足
- **THEN** code=500，msg 含缺货 SKU 名称与剩余可购数量
