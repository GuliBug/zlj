# Proposal: 2026-10-22-align-prototype-and-fix-order-detail

## Why

用户实测反馈三个问题：

1. **订单详情「商品清单」「收货信息」空白（P0 缺陷）**：后端订单详情接口 `orderDetailVo` 未返回 `items` 商品明细，前端渲染为空，买家无法核对所购商品。
2. **H5 与 Web 管理端页面与交互原型差距大**：需求目录存在 `槟榔商城-H5原型.html` 与 `槟榔商城-Web管理端原型.html`，此前实现仅参考了 H5 原型的视觉基调，未逐页对齐其结构、话术与交互，用户明确要求「按照原型来输出」。
3. **种子数据与原型不符**：门店/分类/商品命名与原型（正隆记总号 3 门店、口粮/进阶/待客/礼盒典藏四档、三号究/六号究/九号究/头道究/正隆究系列）不一致。

## What Changes

- **[FIX] order**：`orderDetailVo` 补充 `items` 明细（skuId/name/image/flavor/spec/price/quantity/subtotal）；`itemVos` 补 `skuId`/`subtotal` 字段。
- **[ALIGN] h5-client**：按 H5 原型逐页重做 17 页——品牌导航（印章+门店切换+四正条）、健康警示条、hero+四宫格、分档 chips、红金商品卡（GoodsMedia 统一品牌占位防破图）、详情四正本纪块、购物车原型化、结算 .seg 分段、订单 6 Tab（含待自提）、订单详情动态核销码、我的掌柜卡+四格统计、新增「门店信息」「门店核销码」两页、TabBar 改 4 项。
- **[ALIGN] web-admin**：按 Web 原型重做——侧栏 7 菜单（审计与告警移出侧栏改为工作台入口）、主色 #8f1d22、工作台 4 指标卡+今日概览表+资损监控、商品新增改独立页（三步步骤条+快捷生成+批量填充+违禁词实时校验+库存货值列）、门店营业时间+核销记录面板、订单超 24h 醒目提示、售后超 48h 提示+原路退回话术、会员等级分布卡+规则设置弹窗。
- **[ALIGN] catalog**：种子数据对齐原型——3 家门店（含营业时间字段 `operatingHours` 新增）、4 档分类、6 商品（三号究/六号究/九号究/头道究/正隆究·1000果/礼盒装，默认下架礼盒）；等级名对齐（银卡掌柜/金卡掌柜/钻卡掌柜）。
- **[FIX] catalog**：商品搜索 keyword 增加品牌匹配（PRD：按名称/品牌搜索）。

## Impact

- Affected specs: `order`（详情返回 items）、`h5-client`（页面结构对齐原型）、`web-admin`（页面结构对齐原型）、`catalog`（种子数据与搜索）、`store`（营业时间字段）
- Affected code: `OrderService`、`ProductService`、`Store`/`StoreService`/`DemoDataInitializer`、frontend-h5 全页面、frontend-web 全页面、tests/e2e/visual-check2.mjs
- 回归结果：接口级 E2E 89/89、UI 级 UC 6/6、双端构建通过、截图核验对齐
