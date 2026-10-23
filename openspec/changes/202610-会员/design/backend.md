## Context

会员域实现于 `MemberController/MemberService`（登录、me、积分流水）、`AddressService`（收货地址）与 `AdminMemberController`（会员列表/人工调整积分/等级规则）。等级由累计实付驱动（V0~V3，阈值在等级规则表）；积分以流水账本记录。

## Goals / Non-Goals

**Goals:**

- 记录会员域基线的关键设计口径，作为后续增量（如等级玩法调整）的决策基线。

**Non-Goals:**

- 不改动任何代码；不引入新的等级玩法或积分规则。

## Decisions

### BD1 演示验证码固定 123456

模拟短信通道，登录即注册；生产接入前需替换为真实短信服务（契约结构不变）。

### BD2 等级只升不降

- 备选 A：退款回退等级 → 会员已享权益被回收，客诉风险高。
- 备选 B：重算时目标等级高于当前才更新。
- **取舍：选 B**。实现与口径都简单，符合线下门店习惯。

### BD3 积分记账到流水

每笔变动生成 `{change(±), balanceAfter, reason, relatedNo}`，不可删改；退款按比例扣回；人工调整 reason 前缀「人工调整: 」并留审计。

### BD4 默认地址唯一化在服务层完成

设默认/新增默认时先清除旧默认（`clearDefault`），不依赖前端互斥；上限 20 条由 `Constants.ADDRESS_MAX` 控制。

## Risks / Trade-offs

- [固定验证码可被任意登录] → 仅演示环境可接受；生产前必须接入真实短信（proposal 非目标已声明）。

## Open Questions

（无）
