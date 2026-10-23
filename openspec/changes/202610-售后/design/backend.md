## Context

售后域实现于 `AfterSaleController/AfterSaleService`（买家申请/列表/详情/撤销/寄回物流）与 `AdminAfterSaleController`（审核/确认收货/退款），流转记录 `AfterSaleFlow` 双端同源。

## Goals / Non-Goals

**Goals:**

- 记录售后域基线设计口径（准入窗口、流转留痕、退款幂等）。

**Non-Goals:**

- 不改动任何代码；不做退款渠道对接（演示环境账面退款）。

## Decisions

### BD1 申请准入集中校验

仅 COMPLETED/SHIPPED 订单可申请、签收 7 天内、退货退款需未拆封口径由页面说明承载、每订单每 SKU 限 3 次——全部服务端校验，前端提示仅作引导。

### BD2 流转记录只追加

每次流转（申请/审核/寄回/确认收货/退款/撤销）生成 flow 记录（时间/操作方/动作/备注），不可删改；买家详情与订单详情时间轴（「售后 · 」前缀合并）同源读取。

### BD3 退款幂等与积分联动

退款单号 = 售后单号 + 序号，重复调用返回成功不重复退款；退款金额服务端计算，成功后订单保持 COMPLETED、积分按退款比例扣回并生成负向流水。

### BD4 审核分支与意见必填

审核意见必填（后端校验）；通过：REFUND_ONLY 直接触发退款，RETURN_REFUND → WAIT_RETURN（买家填寄回物流 → 商家确认收货 → 退款 + 库存回补）；拒绝 → REJECTED（可重新申请）。

## Risks / Trade-offs

- [凭证图片为占位勾选] → 演示环境无对象存储；生产需接入文件服务并补图片校验（质量问题时必填已做服务端校验）。

## Open Questions

（无）
