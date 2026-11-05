## 1. 登录与会员（MemberController/MemberService）

- [x] 1.1 POST /api/member/login：验证码固定 123456、新手机号自动注册 V0、返回 MBR- 令牌与会员信息；GET /api/member/me：脱敏手机号/等级/积分/累计实付/下一档差距
- [x] 1.2 GET /api/member/points/logs：积分流水分页（change ±/balanceAfter/reason/relatedNo）
- [x] 1.3 等级自动升档：支付成功与退款成功重算，目标等级高于当前才更新（只升不降），阈值由等级规则表驱动

## 2. 收货地址（AddressController/AddressService）

- [x] 2.1 /api/addresses 增删改查 + POST /{id}/default：必填与 11 位手机号校验、默认地址唯一（clearDefault）、上限 20 条、越权拒绝

## 3. 管理端接口（AdminMemberController）

- [x] 3.1 GET /admin-api/members：keyword 搜索、手机号脱敏
- [x] 3.2 POST /admin-api/members/{id}/points：人工调整积分（变动值非 0、原因必填、流水前缀「人工调整: 」、留审计）
- [x] 3.3 GET/PUT /admin-api/level-rules：阈值查询与批量更新，变更留审计

## 4. 收尾门禁

- [x] 4.1 `cd backend && ./mvnw -q compile` 零报错
- [x] 4.2 `node tests/e2e/e2e-mall.mjs` 全部用例 PASS，docs/测试用例.md 与 docs/测试报告.md 已更新
