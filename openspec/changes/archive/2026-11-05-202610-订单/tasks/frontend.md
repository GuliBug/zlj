## 1. H5（frontend-h5）

- [x] 1.1 Checkout.vue：快递/自提分段、地址选用（默认兜底）、单店自提锁定、积分抵现开关、预估金额（标注服务端为准）、幂等键提交 + loading 防重、页内「订单已提交」金额明细态、缺货失败弹窗
- [x] 1.2 Orders.vue：6 状态 Tab（支持 tab 参数直达）、订单卡（状态/商品摘要/实付）、行内操作按状态渲染（取消/去支付/出示核销码/确认收货/申请售后）、加载更多
- [x] 1.3 OrderDetail.vue：时间轴、PICKUP_WAIT 动态核销码倒计时刷新、商品清单/自提门店/收货信息/物流/金额明细、吸底操作（取消/支付/确认收货/售后入口/查看售后进度）
- [x] 1.4 PayResult.vue：下单成功引导（快递/自提文案）、pay=1 幂等补一击支付、查看订单/返回首页
- [x] 1.5 收尾：`cd frontend-h5 && npm run build` 零报错

## 2. Web 管理端（frontend-web）

- [x] 2.1 views/order/index.vue：状态/关键字筛选、超时未发货高亮、发货弹窗、CSV 批量导入发货（失败回显）、订单导出
- [x] 2.2 收尾：`cd frontend-web && npm run build` 零报错；`node tests/e2e/ui-e2e-web.mjs` 全 PASS

## 3. 缺陷修复补录（2026-10-26）

- [x] 3.1 （BUG-11）OrderDetail.vue `recv` computed 字段形态兼容：`x.address` 为字符串时构造 `{ receiver, phone, addressText }`，不再当对象取属性（FD7）【验证：UI E2E 订单详情收货信息无 undefined，快递单实拍 recv="售后预备 13900002222 湖南省湘潭市 UI 售后预备地址 1 号"】
- [x] 3.2 收尾：`cd frontend-h5 && npm run build` 零报错；`node tests/e2e/ui-e2e.mjs` 22/22 PASS（UC-13 补强断言 noUndef=true）；docs/测试报告.md 登记 BUG-11
