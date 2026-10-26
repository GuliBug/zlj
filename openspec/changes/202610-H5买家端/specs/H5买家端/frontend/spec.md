## ADDED Requirements

### Requirement: H5-01 页面清单
SHALL 提供：首页（品牌导航+分类导航+商品列表）、分类/搜索、商品详情（SKU 弹层）、购物车、确认订单（快递/自提切换）、支付结果、订单列表/详情、售后申请/详情（时间线）、地址列表/编辑、会员中心、个人中心、门店信息（纯展示）。

#### Scenario: 页面可达
- **WHEN** 买家从首页进入分类/详情/购物车/我的
- **THEN** 页面清单内全部页面可正常路由到达

### Requirement: H5-02 详情页交互
SKU 面板 SHALL 底部弹出；缺货 SKU 置灰不可选；已下架时吸底栏替换为「商品已下架」置灰不可点；警示语条灰底不可关闭；「不向未成年人销售」固定展示。

#### Scenario: 加购反馈
- **WHEN** 加购成功
- **THEN** Toast 提示且购物车角标 +1

### Requirement: H5-03 结算页
SHALL 展示地址卡片/自提门店选择、商品清单、金额明细（=服务端返回值）、未成年人提示；提交按钮防重复点击（loading 1.5s）；库存不足弹窗列出缺货 SKU。

#### Scenario: 缺货弹窗
- **WHEN** 提交订单时库存不足
- **THEN** 弹窗列出缺货 SKU，不进入支付

### Requirement: H5-04 自提核销码展示
自提订单详情 SHALL 展示 6 位动态核销码并每分钟自动刷新（调用 verify-code 接口倒计时刷新）。

#### Scenario: 核销码自动刷新
- **WHEN** 停留在自提订单详情跨分钟
- **THEN** 倒计时结束后自动刷新为新窗口码

### Requirement: H5-05 售后详情时间线
售后详情 SHALL 展示流转记录时间线（与商家端同源），申请成功后跳转详情页。

#### Scenario: 时间线同源
- **WHEN** 商家审核通过后买家打开售后详情
- **THEN** 时间线展示与商家端一致的最新流转节点

### Requirement: H5-06 地址管理
地址 SHALL 纯文本输入增删改查（无地图定位）、设默认、删除二次确认、上限 20 条。

#### Scenario: 删除地址二次确认
- **WHEN** 买家删除地址
- **THEN** 弹出二次确认，确认后删除

### Requirement: H5-07 视觉
SHALL 采用红金国潮老字号风（品牌印记「正隆記·始于一九一六」、宗旨「以正立本，以究待客」），移动端 375px 基准适配。

#### Scenario: 品牌印记展示
- **WHEN** 打开 H5 首页
- **THEN** 展示「正隆記·始于一九一六」品牌印记与红金国潮视觉

### Requirement: H5-08 页面配置单一来源
H5 工程 SHALL 以 `frontend-h5/src/pages.json` 作为页面配置唯一来源（三段式：window 主题 / pages 页面清单 / tabBar 底部导航，组织方式对齐参考工程 app.config.ts）；路由、页面标题、登录拦截与 TabBar 渲染均由该配置生成，禁止在组件内硬编码页面清单。

#### Scenario: 新增页面零散改动
- **WHEN** 在 pages.json 增加一个页面条目并放入 pages 目录
- **THEN** 路由、标题与（若声明 tab）底部导航自动生效，无需改其他代码

### Requirement: H5-09 吸底操作栏与 TabBar 叠放
当页面同时存在底部 TabBar 与吸底操作栏（`.bottom-bar`）时，吸底操作栏 SHALL 抬升至 TabBar 之上（`bottom: 52px`），页面底部留白 SHALL 同步预留两层高度；任何关键操作按钮（结算/提交订单/支付）SHALL 在 375×667 与 390×844 视口下中心点不被其他元素遮挡（`elementFromPoint` 校验）。

#### Scenario: 购物车结算
- **WHEN** 购物车存在商品并进入购物车页
- **THEN** 底部「结算(N)」按钮完整可见、可点击，位于 TabBar 上方，点击后进入确认订单页

### Requirement: H5-10 前端工具链对齐
H5 工程依赖与脚本 SHALL 对齐参考工程工具链（sass / eslint9 / eslint-plugin-vue / prettier 及版本约束）；因本工程仅交付 H5 端，不引入 `@tarojs/*`（有意架构决策）。

#### Scenario: 工具链一致性
- **WHEN** 在 H5 工程执行 npm run lint
- **THEN** 使用对齐参考工程的 eslint9 + prettier 工具链，0 error 通过

### Requirement: H5-11 页面结构对齐交互原型
H5 各页面 SHALL 以 `D:/projects/zlj/需求/槟榔商城-H5原型.html` 为结构基准：首页含品牌导航（印章+正隆記+当前门店展示+四正条）、健康警示条、hero 与四宫格、分档 chips、双列商品网格；详情页含四正本纪工艺块与健康警示框；TabBar 为 4 项（首页/全部商品/购物车/我的），订单/地址/售后/门店/核销码自「我的」进入。页面数据 SHALL 来自真实接口，不得硬编码原型的示例数据。

#### Scenario: 买家核对订单商品
- **WHEN** 买家进入订单详情
- **THEN** 商品清单按 items 渲染（名称/图/口味规格/数量/小计），收货信息或自提门店完整显示

#### Scenario: 商品图回退
- **WHEN** 商品 image 字段为空或图片 404
- **THEN** 展示渐变红底品牌占位块，不出现破图图标

### Requirement: H5-12 产线标准入口与门禁
H5 工程 SHALL 满足产线标准：index.html 含 viewport-fit/theme-color/Apple meta/描述与品牌 favicon；ESLint（flat config）与 Prettier 检查 0 error；npm test（vitest）覆盖核心工具函数。

#### Scenario: 产线门禁
- **WHEN** 执行 npm test 与 npm run build
- **THEN** 单元测试通过、构建零报错、lint 0 error

### Requirement: H5-13 商品详情多媒体展示（2026-10-26 增量）
商品详情页 SHALL 支持：主图区多图轮播（横向滑动切换、底部圆点指示当前页），单图或图片为空/加载失败时回退渐变红底品牌占位；SHALL 以「商品详情」区块纵向铺满展示管理端配置的详情图 detailImages（最多 6 张，懒加载），字段为空时该区块不渲染。

#### Scenario: 多图轮播切换
- **WHEN** 商品配置了多张主图
- **THEN** 顶部大图区横向滑动可逐张切换，底部圆点同步指示当前图片

#### Scenario: 详情图随商品展示
- **WHEN** 商品配置了 detailImages 并进入详情页
- **THEN** 工艺块下方「商品详情」区块纵向铺满展示全部详情图；未配置时页面不出现该区块

#### Scenario: 订单详情状态标签间距
- **WHEN** 订单详情页「订单进度」标题后随状态标签（如待发货）
- **THEN** 标题与标签之间保持固定间距，标签不被压缩换行
