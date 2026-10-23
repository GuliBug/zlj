## Context

H5 侧：AftersaleNew.vue（类型/原因/描述/凭证占位）、AftersaleList.vue（工单卡片列表）、AftersaleDetail.vue（工单信息 + flow 时间线 + 撤销）；Web 管理端 views/aftersale/index.vue（工单列表/审核弹窗/确认收货/退款）。

## Goals / Non-Goals

**Goals:**

- 记录售后前端基线交互口径（时间线同源、撤销入口条件、审核必填）。

**Non-Goals:**

- 不改动任何代码；不接入真实图片上传。

## Decisions

### FD1 申请页说明前置

页面顶部固定售后说明（7 天未拆封/拆封仅质量问题/自提到店处理），原因下拉选项来自 `/api/aftersales/reasons` 接口而非前端硬编码，保证与后端枚举一致。

### FD2 撤销入口按状态渲染

详情页仅在申请态（APPLYING/PENDING/WAIT_AUDIT/SUBMITTED）展示「撤销申请」，点击二次确认；撤销后刷新时间线。

### FD3 时间线双端同源展示

买家详情 flow 与管理端详情 flow 同源（后端同一接口数据结构），操作方/备注有值才渲染，页内固定说明「记录不可删改」。

### FD4 管理端审核与超时高亮

审核弹窗意见必填；列表超 48h 未审核行高亮（auditOverdue）；退款按钮二次确认；退款金额只读展示服务端计算值。

### FD5 契约疑问章节

无疑问：售后字段与 flow 结构和契约一致。

## Risks / Trade-offs

- [凭证为勾选占位] → 管理端凭证区同步展示占位信息，演示链路完整；生产接入图片后无需改交互结构。

## Open Questions

（无）
