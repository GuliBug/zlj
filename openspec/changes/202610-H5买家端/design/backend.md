## Context

「H5买家端」能力为 frontend-h5（Vue3 + vue-router，无 Taro）；买家后端语义归各能力域（会员 AC-01/AC-05、商品 AC-03、购物车 AC-04、订单 AC-06/AC-07、售后 AC-08、门店 AC-02）。本文件记录归属边界；无后端改动。

## Goals / Non-Goals

**Goals:**

- 说明 H5 前端与其后端能力的归属边界，避免增量需求落错 change。

**Non-Goals:**

- 不改动任何代码；不在本能力内新增后端接口（买家接口变更一律归入对应能力域）。

## Decisions

### BD1 买家接口归属能力域

/api/* 控制器按业务域拆分，H5买家端 change 仅承载页面渲染与交互口径；接口行为变更落在对应能力域 change，页面消费映射见本 change api.md。

### BD2 合规底线不依赖前端

警示语/未成年人提示/违禁词由后端硬编码返回或服务端校验，前端仅渲染——前端被绕过也不破坏合规底线。

## Risks / Trade-offs

- [页面交互与接口口径分散两处] → H5买家端 spec 收页面行为（H5-xx），能力域 spec 收接口行为（FR-xx/AC-xx），编号互查。

## Open Questions

（无）
