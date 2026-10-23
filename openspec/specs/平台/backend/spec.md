# 平台 Specification — backend

## Purpose
平台层横切规格（单端能力：系统侧）：登录态与安全、审计日志、资金安全三件套（幂等/对账/资损监控）、定时任务、数据留存、敏感信息脱敏、前端工程治理与测试门禁，覆盖 PRD 第七节非功能与第十节安全要求。

## Requirements

### Requirement: PL-01 登录态
买家/管理员令牌 SHALL 经 X-Token 传递（MBR-/ADM- 前缀），有效期 7 天，拦截器按 @RequireLogin 校验；退出登录由前端清除本地 token。

#### Scenario: 令牌过期
- **WHEN** X-Token 超过 7 天有效期后请求需登录接口
- **THEN** 返回未登录错误，前端清除本地 token

### Requirement: PL-02 管理端登录安全
管理端账密登录 SHALL 连续 5 次失败锁定 30 分钟。

#### Scenario: 连续失败锁定
- **WHEN** 连续 5 次错误密码登录
- **THEN** 账号锁定 30 分钟，期间正确密码也被拒

### Requirement: PL-03 审计日志
资金相关与商品上下架、改价、积分调整、盘点确认、阈值修改 SHALL 记录审计日志（操作人/时间/IP/动作/变更前后值），不可删改。

#### Scenario: 改价留审计
- **WHEN** 管理端修改商品售价
- **THEN** 审计日志新增记录（操作人/时间/IP/变更前后值）

### Requirement: PL-04 资损监控
SHALL 实现四类告警：重复支付（同订单多笔成功）、金额异常（>5000 元）、状态回跳、对账差异；告警入 alerts 表可查询。

#### Scenario: 重复支付告警
- **WHEN** 已支付订单再次支付成功路径被触发
- **THEN** 登记 DUPLICATE_PAY 告警

### Requirement: PL-05 定时任务
SHALL 有 30s 扫描的订单超时关单任务（15 分钟）与日终库存核对任务（实物=Σ流水，差异告警）。

#### Scenario: 日终库存核对
- **WHEN** 日终核对任务执行
- **THEN** 逐 SKU 校验实物=Σ流水，差异登记 RECON_DIFF 告警

### Requirement: PL-06 数据留存
订单与资金流水、审计日志 SHALL 永久保留（H2 演示环境生命周期内；生产 MySQL 留存 ≥5 年/≥2 年为部署要求）。

#### Scenario: 流水不清理
- **WHEN** 系统长期运行
- **THEN** 订单/资金流水/审计日志始终可查（演示环境生命周期内不清理）

### Requirement: PL-07 敏感信息脱敏
手机号 SHALL 存储明文（演示）但展示脱敏（138****1114），管理端会员列表与订单查询一致。

#### Scenario: 手机号脱敏展示
- **WHEN** 管理端会员列表或订单查询展示手机号
- **THEN** 按 138****1114 形式脱敏

### Requirement: PF-05 前端依赖对齐参考工程
双前端工程 package.json SHALL 与 qingguoji-mall 对应工程逐版本对齐依赖与脚本命名（web 全量对齐；h5 对齐工具链，Taro 除外并记录偏差）；升级依赖时 SHALL 在 openspec change 中留档。

#### Scenario: 依赖升级留档
- **WHEN** 升级任一前端依赖版本
- **THEN** 在 openspec change 中留档说明偏差

### Requirement: PF-06 质量门禁
工程 SHALL 建立三层测试体系并作为回归门禁：单元测试（vitest 3.2.4，双端 npm test）、UI 级端到端（puppeteer 24.10.0 真实浏览器，H5 + Web 管理端）、接口级端到端（e2e-mall.mjs 真实 HTTP 对账全部接口）；双端 SHALL 通过 ESLint（flat config）与 Prettier 检查（0 error）。测试结果自动并入 docs/测试用例.md 与 docs/测试报告.md。

#### Scenario: 回归门禁全绿
- **WHEN** 执行 npm test、node tests/e2e/ui-e2e.mjs、ui-e2e-web.mjs、e2e-mall.mjs
- **THEN** 全部通过且结果自动并入 docs/测试用例.md 与 docs/测试报告.md
