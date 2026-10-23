## Context

「管理端」能力为 Web 管理端前端（frontend-web）；其后端语义归各能力域（商品 AC-10/库存 AC-11/门店 AC-12/订单 AC-13/售后 AC-14/会员 AC-15/平台 AC-09/AC-16）。本文件记录管理端专属横切口径；无后端改动。

## Goals / Non-Goals

**Goals:**

- 说明管理端前端与其后端能力的归属边界，避免增量需求落错 change。

**Non-Goals:**

- 不改动任何代码；不在本能力内新增后端接口（管理端新接口一律归入对应能力域）。

## Decisions

### BD1 后端接口归属能力域

Admin*Controller 按业务域拆分（AdminProductController → 商品、AdminStockController → 库存……），管理端 change 仅承载页面/交互/结构口径；接口行为变更必须落在对应能力域的 change。

### BD2 登录与看板归属平台能力

登录安全（锁定策略）、审计与告警接口（AC-09/AC-16）归「平台」能力；管理端 change 消费之。

## Risks / Trade-offs

- [能力边界易混淆] → 各 change 的 api.md 已标注接口归属（AC 编号），增量需求按「改的是接口还是页面」选择落点。

## Open Questions

（无）
