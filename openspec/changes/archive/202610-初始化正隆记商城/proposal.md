# Proposal: 202610-初始化正隆记商城

## Why

为「正隆记」品牌搭建线上槟榔商城：H5 商城端（买家）+ Web 管理端（门店运营）+ Spring Boot 后端。需求来源为 PRD v1.8（FR-01~FR-16）。采用 OpenSpec spec-driven 流程管理本仓库，本 change 是基线：一次性建立主规格库与全部工程骨架。

## What Changes

- **ADDED** 11 个能力：`membership` / `catalog` / `cart` / `order` / `aftersale` / `stock` / `store` / `web-admin` / `h5-client` / `platform` / `api-contract`
- 新建三端工程：`backend/`（Spring Boot 3.5 + JPA + H2）、`frontend-h5/`（Vue3 + Vite）、`frontend-web/`（Vue3 + Element Plus + Vite）
- 新建 `tests/e2e/` 端到端自动化对账与 `docs/` 测试用例、测试报告

## Impact

- 触达需求编号：FR-01~FR-08、FR-10~FR-14、FR-16（FR-15 优惠券已移除，编号占位）
- Affected specs：全部 11 个能力（新增）

## Out of Scope（本期不做）

- 优惠券模块（FR-15，v1.5 移除）、商品资质字段（v1.8 移除）、售卖区域限制（v1.4 移除）、RBAC/合规中心（v1.2 移除）
- 真实微信支付/支付宝对接（RK-01 类目报备未决，本期模拟通道，保留幂等与双通道确认语义）
- 真实短信服务商（验证码固定 123456）、真实物流轨迹接口（展示物流公司+单号）
- App / 小程序端、发票、拼团/秒杀等营销玩法（后续版本池）
