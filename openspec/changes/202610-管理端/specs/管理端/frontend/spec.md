## ADDED Requirements

### Requirement: FR-16-1 数据看板
工作台 SHALL 展示 GMV、订单数、支付成功率、客单价、售后率、退款金额、库存预警数、近 7 日趋势（T+1 口径简化为实时）。

#### Scenario: 看板指标渲染
- **WHEN** 管理员进入工作台
- **THEN** 展示 GMV/订单数/支付成功率/客单价/售后率/退款金额/库存预警数与近 7 日趋势

### Requirement: FR-12-1/FR-12-4 订单列表
订单列表 SHALL 支持按状态/订单号/手机号（脱敏）筛选；待发货超 24 小时高亮提醒。

#### Scenario: 超时未发货高亮
- **WHEN** 存在 PAID 状态超 24 小时未发货订单
- **THEN** 列表对应行 shipOverdue=true 并高亮提醒

### Requirement: FR-12-2 发货
发货 SHALL 填物流公司+单号，仅 PAID 可发；支持 CSV 导入批量发货，失败行回显原因。

#### Scenario: 批量发货失败回显
- **WHEN** 批量发货含不存在的订单号
- **THEN** 成功行生效，失败行返回原因

### Requirement: FR-12-3 订单导出
SHALL 支持按时间范围导出订单 CSV（含金额明细），仅管理员可用。

#### Scenario: 按时间导出
- **WHEN** 管理员按时间范围导出订单
- **THEN** 下载含金额明细列的 CSV 文件

### Requirement: FR-10-4 商品列表筛选
商品列表 SHALL 支持分类/状态/名称/条码筛选与分页，支持导出 CSV。

#### Scenario: 组合筛选
- **WHEN** 按分类与名称关键字组合筛选商品列表
- **THEN** 返回匹配商品的分页列表

### Requirement: FR-13-1 售后工单
售后详情 SHALL 展示买家申请信息、凭证、服务端只读退款金额与完整流转时间线；审核通过/拒绝后实时追加记录。

#### Scenario: 审核后时间线实时追加
- **WHEN** 管理端审核通过售后工单
- **THEN** 详情流转时间线实时追加审核记录

### Requirement: FR-14-1/FR-14-2 会员管理
会员列表 SHALL 脱敏展示手机号/等级/积分/累计消费；等级阈值可配置（留审计）；含等级分布卡与规则设置入口。

#### Scenario: 会员脱敏展示
- **WHEN** 打开会员列表
- **THEN** 手机号按 138****1114 形式脱敏展示，等级/积分/累计消费可见

### Requirement: FR-14-4 积分人工调整入口
会员列表 SHALL 提供「调整积分」入口：弹窗填写变动值（±，步进 10）与原因（必填），提交前双重确认（两道确认框，第二道为错误级「最终确认」）；成功后刷新列表与统计并提示已留审计。

#### Scenario: 双重确认后提交
- **WHEN** 管理员依次通过两道确认框
- **THEN** 调用调整接口成功后提示「积分调整成功（留审计）」并刷新列表

### Requirement: FR-15-1 管理端登录
Web 管理端 SHALL 提供账密登录页（品牌印章视觉、回车提交、loading 防重、演示账号提示）；成功后 token/用户名入 store 并跳转 redirect 或工作台；失败透传后端提示（含连续 5 次失败锁定 30 分钟文案）。

#### Scenario: 登录成功进入工作台
- **WHEN** 输入有效账密登录
- **THEN** 提示登录成功并跳转 /dashboard

### Requirement: 第十节 审计与告警
管理端 SHALL 提供审计日志查询（操作人/时间/IP/变更前后值）与告警列表（资损四类+库存预警），自工作台资损监控入口进入。

#### Scenario: 上下架留审计
- **WHEN** 运营执行上下架
- **THEN** 审计日志新增记录操作人/时间/IP/动作

### Requirement: 危险操作二次确认
上下架、退款、积分调整、盘点确认、删除地址等危险操作 SHALL 前端二次确认。

#### Scenario: 退款二次确认
- **WHEN** 管理员点击退款操作
- **THEN** 弹出二次确认弹窗，确认后才发起请求

### Requirement: WA-17 依赖与脚本对齐参考工程
Web 管理端 package.json SHALL 逐版本对齐 qingguoji-mall-web：dependencies（@element-plus/icons-vue 2.3.2 / axios 1.13.2 / element-plus 2.13.1 / js-cookie 3.0.5 / nprogress 0.2.0 / pinia 3.0.4 / vue 3.5.26 / vue-router 4.6.4）与 devDependencies（vite 6.4.1 / @vitejs/plugin-vue 5.2.4 / sass 1.83.0 / eslint 9 + prettier 全套）；scripts SHALL 提供 build:prod / preview(5174) / lint / format，与参考工程命名一致。

#### Scenario: 依赖锁定构建
- **WHEN** 按 package.json 固化版本执行 npm run build:prod
- **THEN** 构建成功且依赖版本与参考工程一致

### Requirement: WA-18 页面结构对齐 Web 管理端原型
Web 管理端 SHALL 以 `D:/projects/zlj/需求/槟榔商城-Web管理端原型.html` 为结构基准：侧栏 7 菜单（工作台/商品管理(商品列表+新增商品)/库存/门店/订单/售后/会员），审计与告警自工作台资损监控入口进入；主色 #8f1d22、侧栏 #241d18；工作台含 4 指标卡与今日概览表；新增/编辑商品为独立页面（三步步骤条、SKU 快捷生成与批量填充、违禁词实时校验、库存货值列）；门店页含营业时间维护与核销记录。

#### Scenario: 新增商品三步流程
- **WHEN** 管理员在商品管理点「新增商品」
- **THEN** 进入独立页面按 基本信息→SKU与价格→图片与详情 三步填写，名称/描述输入时实时违禁词校验，命中即红字提示并阻断提交

### Requirement: WA-19 管理端 UI 自动化回归
管理端 SHALL 具备 puppeteer UI 级端到端回归（登录、七个菜单页关键元素渲染、新增商品违禁词实时拦截、全程无页面 JS 异常），测试脚本依赖本工程 node_modules（puppeteer 24.10.0）自包含运行。

#### Scenario: UI 回归全绿
- **WHEN** 运行 `node tests/e2e/ui-e2e-web.mjs`（需后端与 Web dev 已启动）
- **THEN** 登录/七菜单页/违禁词拦截断言全部 PASS，无页面 JS 异常
