## 1. H5（frontend-h5）

- [x] 1.1 Login.vue：手机号/验证码前端校验、演示提示、redirect 回跳、loading 防重、登录后刷新购物车角标
- [x] 1.2 Member.vue：掌柜卡（等级/脱敏手机号/累计消费）、四格统计（积分/地址数/核销码入口/订单数）、菜单列表、积分明细弹层（正负分色）、退出登录、未登录兜底
- [x] 1.3 VerifyCode.vue：无 orderId 引导态 + 有 orderId 动态码倒计时自动刷新（两位分组展示）
- [x] 1.4 Addresses.vue / AddressEdit.vue：增删改查、设默认、删除二次确认、结算选用态（from=checkout）
- [x] 1.5 收尾：`cd frontend-h5 && npm run build` 零报错

## 2. Web 管理端（frontend-web）

- [x] 2.1 views/member/index.vue：会员列表（脱敏/等级/积分/累计消费）、等级分布卡、等级规则设置弹窗（留审计提示）、调整积分弹窗（双重确认）
- [x] 2.2 收尾：`cd frontend-web && npm run build` 零报错；`node tests/e2e/ui-e2e-web.mjs` 全 PASS
