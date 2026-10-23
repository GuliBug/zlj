## Context

H5 侧为 Login.vue（验证码登录）、Member.vue（掌柜卡 + 四格统计 + 菜单）、VerifyCode.vue（门店核销码两态页）；Web 管理端为 views/member/index.vue（会员列表 + 等级规则弹窗 + 调整积分弹窗）。

## Goals / Non-Goals

**Goals:**

- 记录会员域前端基线交互口径（登录回跳、角标刷新、脱敏展示、危险操作确认）。

**Non-Goals:**

- 不改动任何代码；不新增页面。

## Decisions

### FD1 登录成功回跳 redirect

登录页读取 `route.query.redirect`，成功后 `router.replace(redirect || '/member')`；401 全局拦截跳登录时携带 redirect，登录后回到原页面。

### FD2 会员中心四格统计来自真实接口

积分来自 `/api/member/me`，地址数来自 `/api/addresses`，订单数来自 `/api/orders?page=1&size=1` 的 total；积分明细弹层取 `points/logs` 前 5 条，正负分色。

### FD3 积分人工调整双重确认

Web 调整积分弹窗需连过两道 `ElMessageBox.confirm`（第二道为 error 级「最终确认」），原因必填；成功后提示「留审计」并刷新列表与统计。

### FD4 契约疑问章节

无疑问：接口返回与契约一致；手机号脱敏由后端完成，前端原样展示。

## Risks / Trade-offs

- [四格统计并行请求失败] → 各自 `.catch(() => {})` 静默兜底显示 0，不阻塞主卡渲染。

## Open Questions

（无）
