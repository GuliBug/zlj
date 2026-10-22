# api-contract Specification

## Purpose
前后端唯一契约来源。统一响应包装 `AjaxResult{code,msg,data}`（code=200 成功）；分页参数 `page`(1 起)/`size`(默认10)，分页数据 `{total,rows}` 放在 data 内。登录态经请求头 `X-Token` 传递（买家前缀 MBR-，管理员前缀 ADM-）。金额一律整数「分」。错误响应 `{code:500, msg:"..."}`，业务校验失败 code=500。时间格式 `yyyy-MM-dd HH:mm:ss`。

## ADDED Requirements

### Requirement: AC-01 会员接口 /api/member
- `POST /api/member/login` body `{phone, smsCode}` → `data:{token, memberId, phone, levelName, points}`；验证码固定 123456；新手机号自动注册。错误验证码 code≠200。
- `GET /api/member/me` → `data:{id, phone(脱敏138****1111), levelCode(V0|V1|V2|V3), levelName, points, totalPaid(分), nextLevelName, nextLevelGap(分)}`
- `GET /api/member/points/logs?page=&size=` → `data:{total, rows:[{id, change(±), balanceAfter, reason, relatedNo, createdAt}]}`

#### Scenario: 登录返回令牌
- **WHEN** POST /api/member/login 传正确验证码
- **THEN** 返回 MBR- 前缀 token 与会员信息

### Requirement: AC-02 门店接口 /api/stores
- `GET /api/stores` → `data:[{id, name, address, phone, status(OPEN|CLOSED)}]`

### Requirement: AC-03 商品接口 /api/products
- `GET /api/categories` → `data:[{id, name, parentId, children:[...]}]`
- `GET /api/products?keyword=&categoryId=&page=&size=` → `data:{total, rows:[{id, name, image, priceMin(分), priceMax, sales, status(ON|OFF)}]}`（仅上架商品对买家可见；keyword 支持名称/条码）
- `GET /api/products/{id}` → `data:{id, name, brand, origin, description, images[], categoryId, categoryName, status, warnText(固定「长期过量咀嚼槟榔，有害口腔健康」), minorText(固定「不向未成年人销售」), skus:[{id, flavor, spec, barcode, price(分), stock(可用), sales}]}`；下架商品 status=OFF 仍可查详情但禁止下单
- `GET /api/products/{id}/remind` → 到货提醒登记 → `data:"ok"`

### Requirement: AC-04 购物车接口 /api/cart
- `GET /api/cart` → `data:{rows:[{id, skuId, productId, name, image, flavor, spec, price(分), quantity, stock, checked, invalid(下架/失效标识)}], totalQuantity}`
- `POST /api/cart` body `{skuId, quantity}` → 加购（合并同 SKU，单 SKU 上限 999，购物车 SKU 种类上限 100 超限报「购物车已满」）
- `POST /api/cart/update` body `{id, quantity?, checked?}` 
- `POST /api/cart/delete` body `{ids:[...]}`
- `POST /api/cart/check-all` body `{checked}`
- `POST /api/cart/clear-invalid` → 清除失效商品

### Requirement: AC-05 地址接口 /api/addresses
- `GET /api/addresses` → `data:[{id, receiver, phone, region, detail, isDefault}]`
- `POST /api/addresses` body `{receiver, phone, region, detail, isDefault}`（上限 20 条）
- `PUT /api/addresses/{id}`；`DELETE /api/addresses/{id}`；`POST /api/addresses/{id}/default`

### Requirement: AC-06 订单接口 /api/orders
- `POST /api/orders` body `{idempotentKey(UUID), fulfillmentType(EXPRESS|PICKUP), storeId?(PICKUP必填), addressId?(EXPRESS必填), usePoints(bool), items:[{skuId, quantity}]}` → `data:{orderNo, goodsAmount(分), freight(分), pointsDeduction(分), payAmount(分), pointsUsed, status}`
  - 服务端计算全部金额；运费：EXPRESS 满 9900 分包邮否则 1000 分，PICKUP 为 0；积分抵现 100 积分=1 元且 ≤ payAmount 前 5%（抵扣前金额）；单笔 payAmount ≤ 500000 分
  - 幂等：同 idempotentKey 5 分钟内重复提交返回首次订单（data 相同），不重复创建
  - 库存不足：code=500，msg 含缺货 SKU 名称与剩余可购数量
- `GET /api/orders?status=&page=&size=` → `data:{total, rows:[{orderNo, status, statusText, payAmount, createdAt, items:[{name, image, flavor, spec, price, quantity}], fulfillmentType}]}`；status 枚举 WAIT_PAY/PAID/SHIPPED/PICKUP_WAIT/COMPLETED/CLOSED/AFTER_SALE
- `GET /api/orders/{no}` → 订单详情：含金额明细、收货信息/自提门店、物流信息 `{logisticsCompany, logisticsNo}`、时间轴 `timeline:[{action, time, remark}]`、PICKUP_WAIT 态含 `pickupCode`（服务端算当前分钟窗口码）
- `POST /api/orders/{no}/pay` → 模拟支付成功；非 WAIT_PAY 拒绝（幂等）
- `POST /api/orders/{no}/cancel` → 仅 WAIT_PAY 可取消，释放预占
- `POST /api/orders/{no}/confirm` → 仅 SHIPPED 可确认收货 → COMPLETED

#### Scenario: 幂等下单
- **WHEN** 同 idempotentKey 5 分钟内重复提交
- **THEN** 返回首次订单，不产生重复订单

#### Scenario: 库存预占失败
- **WHEN** 可用库存不足
- **THEN** 下单失败且返回缺货 SKU 与剩余可购数量，库存不变

### Requirement: AC-07 核销码
- `GET /api/orders/{no}/verify-code` → `data:{code(6位), expiresInSec(≤60)}`；码按分钟窗口刷新；核销校验接受当前窗口与上一窗口码

#### Scenario: 码每分钟刷新
- **WHEN** 跨分钟两次取码
- **THEN** 返回不同 code

### Requirement: AC-08 售后接口 /api/aftersales
- `GET /api/aftersales/reasons` → `data:[...]`（原因枚举：质量问题/少件漏发/发错货/不想要了/其他）
- `POST /api/aftersales` body `{orderNo, type(REFUND_ONLY|RETURN_REFUND), reason, description?, images?[]}` → `data:{afterSaleNo}`；仅 COMPLETED/SHIPPED 订单可申请（签收后 7 天内）；每订单每 SKU 限 3 次
- `GET /api/aftersales?page=&size=` → `data:{total, rows:[{afterSaleNo, orderNo, type, typeText, status, statusText, refundAmount, createdAt}]}`
- `GET /api/aftersales/{no}` → 详情含 `flow:[{action, operator, remark, time}]`（流转时间线，只读不可删改）
- `POST /api/aftersales/{no}/cancel` → 仅商家审核前可撤销
- `POST /api/aftersales/{no}/return-logistics` body `{company, no}` → 退货退款态买家填寄回物流

#### Scenario: 售后流转记录追加
- **WHEN** 买家提交/商家审核/退款完成等任一节点发生
- **THEN** flow 追加一条记录，双端可见，不可删改

### Requirement: AC-09 管理端登录与看板 /admin-api
- `POST /admin-api/login` body `{username, password}` → `data:{token(ADM-), username, nickname}`（种子 admin/admin123）
- `GET /admin-api/dashboard` → `data:{gmvToday(分), orderCountToday, payRate, pendingShip, pendingAftersale, stockAlertCount, salesTrend:[{date, gmv, orderCount}](近7日)}`

### Requirement: AC-10 管理端商品 /admin-api/products
- `GET /admin-api/products?keyword=&status=&categoryId=&page=&size=` → `data:{total, rows:[...含 skus 摘要]}`
- `GET /admin-api/products/{id}`；`POST /admin-api/products` body `{name, categoryId, brand, origin, description, images[], detailImages[], skus:[{flavor, spec, barcode, price, costPrice, stock}]}`（违禁词命中 code=500 msg 列出命中词；条码重复校验）
- `PUT /admin-api/products/{id}`（改价对未支付订单不生效；变更留审计）
- `POST /admin-api/products/status` body `{ids:[], status(ON|OFF)}` → 批量上下架，留审计
- `POST /admin-api/check-banned-words` body `{text}` → `data:{hits:[...]}`
- `GET /admin-api/categories`；`POST /admin-api/categories` body `{name, parentId}`

### Requirement: AC-11 管理端库存 /admin-api/stock
- `GET /admin-api/skus?keyword=&page=&size=` → `data:{total, rows:[{id, productName, flavor, spec, physical, available, reserved, frozen, threshold, status(NORMAL|FROZEN)}]}`
- `POST /admin-api/stock/inbounds` body `{skuId, quantity(>0), batchNo(必填), supplier?, remark?}` → 生成入库流水
- `GET /admin-api/stock/inbounds?page=` → 入库单列表
- `POST /admin-api/stock/freeze` body `{skuId, action(FREEZE|RELEASE), reason}` → 冻结后可用清零、预占不受影响；生成流水
- `POST /admin-api/stock/threshold` body `{skuId, threshold}` → 留审计
- `GET /admin-api/stock/flows?skuId=&type=&page=&size=` → `rows:[{id, skuId, productName, type(IN|OUT|RESERVE|RELEASE|REFUND_IN|ADJUST|FREEZE|UNFREEZE), quantity, balanceAfter, relatedNo, remark, createdAt}]`
- `POST /admin-api/stock/stocktakes` body `{items:[{skuId, actual}], remark}` → 创建盘点单（差异自动计算，状态 PENDING_CONFIRM）
- `GET /admin-api/stock/stocktakes?page=` → `rows:[{id, status(PENDING_CONFIRM|CONFIRMED), items:[{skuId, productName, systemCount, actual, diff}], remark, createdBy, createdAt, confirmedBy}]`
- `POST /admin-api/stock/stocktakes/{id}/confirm` → 二级确认（提交人≠确认人，违反 code=500）；确认后按差异修正库存并生成 ADJUST 流水

#### Scenario: 盘点二级确认
- **WHEN** 提交人本人确认盘点单
- **THEN** 系统拒绝（提交人不得同时担任确认人）

### Requirement: AC-12 管理端门店与核销 /admin-api/stores
- `GET /admin-api/stores`；`POST /admin-api/stores` body `{name, address, phone, status}`；`PUT /admin-api/stores/{id}`
- `GET /admin-api/verify-records?page=&size=&storeId=` → `rows:[{id, orderNo, storeName, verifier(店员), verifyTime, payAmount}]`

#### Scenario: 到店核销
- **WHEN** 店员提交待自提订单核销码（码=当前或上一分钟窗口码且门店匹配）
- **THEN** 订单→COMPLETED、库存实扣、生成核销记录；码无效/门店不匹配返回明确失败原因

- `POST /admin-api/verify` body `{orderNo, code, storeId}` → 核销（校验：订单状态=PICKUP_WAIT、码有效、门店匹配）→ 订单 COMPLETED + 库存实扣 + 核销记录 + 流转时间轴

### Requirement: AC-13 管理端订单 /admin-api/orders
- `GET /admin-api/orders?status=&keyword=&page=&size=&overdueShip=` → keyword 匹配订单号/手机号（脱敏）；待发货超 24h 行返回 `shipOverdue:true`
- `POST /admin-api/orders/{no}/ship` body `{company, no}` → 仅 PAID 可发货
- `POST /admin-api/orders/batch-ship` body `{rows:[{orderNo, company, no}]}` → 返回 `{success:n, failures:[{orderNo, reason}]}`
- `GET /admin-api/orders/export?from=&to=` → CSV 文本（text/csv；列：订单号/状态/商品/金额/支付方式/履约方式/下单时间）

### Requirement: AC-14 管理端售后 /admin-api/aftersales
- `GET /admin-api/aftersales?status=&page=&size=&overdue=` → 超 48h 未审核行 `auditOverdue:true`
- `GET /admin-api/aftersales/{no}` → 详情含买家申请信息+flow 时间线
- `POST /admin-api/aftersales/{no}/audit` body `{approve(bool), opinion(必填)}` → 通过：REFUND_ONLY 直接触发退款；RETURN_REFUND → WAIT_RETURN；拒绝 → REJECTED（可重新申请）
- `POST /admin-api/aftersales/{no}/confirm-return` → 退货退款确认收货 → 触发退款 + 库存回补
- `POST /admin-api/aftersales/{no}/refund` → 幂等退款（重复调用不重复退款）；成功后售后单→REFUNDED、订单→COMPLETED 保持、积分按比例扣减

#### Scenario: 退款幂等
- **WHEN** 对同一售后单重复调用 refund
- **THEN** 第一次成功，后续调用返回成功但不重复退款（退款序号不增）

### Requirement: AC-15 管理端会员 /admin-api/members
- `GET /admin-api/members?keyword=&page=&size=` → 手机号脱敏
- `POST /admin-api/members/{id}/points` body `{change(±), reason(必填)}` → 双重确认由前端承担；留审计
- `GET /admin-api/level-rules` → `data:[{levelCode, levelName, threshold(累计实付分)}]`；`PUT /admin-api/level-rules` body `[{levelCode, threshold}]` → 留审计

### Requirement: AC-16 审计与监控 /admin-api
- `GET /admin-api/audit-logs?page=&size=&module=` → `rows:[{id, operator, module, action, detail(变更前后值), ip, createdAt}]`
- `GET /admin-api/alerts?page=` → 资损/库存告警列表 `rows:[{id, type(DUPLICATE_PAY|ABNORMAL_AMOUNT|STATUS_ROLLBACK|RECON_DIFF|STOCK_ALERT), content, status, createdAt}]`
