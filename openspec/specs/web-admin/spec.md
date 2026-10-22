# web-admin Specification

## Purpose
Web 管理端页面层规格：工作台、商品、库存三页签、门店、订单、售后、会员、审计与告警展示，覆盖 FR-10~FR-14、FR-16、第十节安全底线。

## Requirements

### Requirement: FR-16-1 数据看板
工作台 SHALL 展示 GMV、订单数、支付成功率、客单价、售后率、退款金额、库存预警数、近 7 日趋势（T+1 口径简化为实时）。

### Requirement: FR-12-1/FR-12-4 订单列表
订单列表 SHALL 支持按状态/订单号/手机号（脱敏）筛选；待发货超 24 小时高亮提醒。

### Requirement: FR-12-2 发货
发货 SHALL 填物流公司+单号，仅 PAID 可发；支持 Excel 导入批量发货，失败行回显原因（CSV 导入）。

#### Scenario: 批量发货失败回显
- **WHEN** 批量发货含不存在的订单号
- **THEN** 成功行生效，失败行返回原因

### Requirement: FR-12-3 订单导出
SHALL 支持按时间范围导出订单 CSV（含金额明细），仅管理员可用。

### Requirement: FR-10-4 商品列表筛选
商品列表 SHALL 支持分类/状态/名称/条码筛选与分页，支持导出 CSV。

### Requirement: FR-13-1 售后工单
售后详情 SHALL 展示买家申请信息、凭证、服务端只读退款金额与完整流转时间线；审核通过/拒绝后实时追加记录。

### Requirement: FR-14-1/FR-14-2 会员管理
会员列表 SHALL 脱敏展示手机号/等级/积分/累计消费；等级阈值可配置（留审计）。

### Requirement: 第十节 审计与告警
管理端 SHALL 提供审计日志查询（操作人/时间/IP/变更前后值）与告警列表（资损四类+库存预警）。

#### Scenario: 上下架留审计
- **WHEN** 运营执行上下架
- **THEN** 审计日志新增记录操作人/时间/IP/动作

### Requirement: 危险操作二次确认
上下架、退款、积分调整、盘点确认、删除地址等危险操作 SHALL 前端二次确认。
### Requirement: WA-17 依赖与脚本对齐参考工程
Web 管理端 package.json SHALL 逐版本对齐 qingguoji-mall-web：dependencies（@element-plus/icons-vue 2.3.2 / axios 1.13.2 / element-plus 2.13.1 / js-cookie 3.0.5 / nprogress 0.2.0 / pinia 3.0.4 / vue 3.5.26 / vue-router 4.6.4）与 devDependencies（vite 6.4.1 / @vitejs/plugin-vue 5.2.4 / sass 1.83.0 / eslint 9 + prettier 全套）；scripts SHALL 提供 build:prod / preview(5174) / lint / format，与参考工程命名一致。

### Requirement: WA-19 管理端 UI 自动化回归
管理端 SHALL 具备 puppeteer UI 级端到端回归（登录、七个菜单页关键元素渲染、新增商品违禁词实时拦截、全程无页面 JS 异常），测试脚本依赖本工程 node_modules（puppeteer 24.10.0）自包含运行。
