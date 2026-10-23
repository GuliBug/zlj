# 正隆记 · 槟榔商城（zhenglongji-mall）

> SPEC 驱动开发（OpenSpec · spec-driven）。需求上游：`D:/projects/zlj/需求/槟榔商城-PRD-v1.0.md`（v1.8）。
> 架构与流程参考 `D:/projects/qingguoji-mall-main`（前后端分离 + OpenSpec）。

## 项目结构

```
zhenglongji-mall/
├── openspec/         # SPEC 驱动开发：config.yaml（schema: zlj-team）+ schemas/zlj-team/ + specs/（11 能力×按端主规格库）+ changes/（archive）
├── backend/          # Spring Boot 3.5 后端（若依分层：common / framework / mall，端口 8080）
├── frontend-h5/      # H5 商城端（Vue3 + Vite，端口 10086，买家；页面配置单一来源 src/pages.json，对齐参考工程 app.config 组织方式）
├── frontend-web/     # Web 管理端（Vue3 + Element Plus + Vite，端口 5173，运营）
├── tests/e2e/        # 端到端自动化对账（Node，真实 HTTP）
├── docs/             # 测试用例、测试报告
├── AGENTS.md         # OpenSpec 协作约定
└── README.md
```

## 快速启动

```bash
# 0. 一键启动（Windows 双击即可，拉起三端）
start-all.cmd

# 1. 后端（需 JDK 21；默认 H2 内存库免装 MySQL）
cd backend && ./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run

# 2. H5 商城端
cd frontend-h5 && npm install && npm run dev   # http://localhost:10086

# 3. Web 管理端
cd frontend-web && npm install && npm run dev  # http://localhost:5173

# 4. 测试（需后端与 dev server 已启动；puppeteer 24.10.0 / vitest 3.2.4 已固化）
cd frontend-h5 && npm test                    # 单元测试（vitest 3.2.4）
cd frontend-web && npm test                   # 单元测试（vitest 3.2.4）
node tests/e2e/e2e-mall.mjs                   # 接口级 89 断言，结果写入 tests/e2e/last-run.json
node tests/e2e/ui-e2e.mjs                     # H5 UI 级 7 断言（真实浏览器，防遮挡回归）
node tests/e2e/ui-e2e-web.mjs                 # Web 管理端 UI 级 11 断言（登录/7 菜单页/违禁词拦截）
node tests/e2e/gen-docs.mjs                   # 重新生成 docs/测试用例.md 与 docs/测试报告.md
```

## 原型对齐

页面结构以需求目录的两个交互原型为单一基准：`槟榔商城-H5原型.html`、`槟榔商城-Web管理端原型.html`。
- H5：品牌导航 / 健康警示条 / 四宫格 / 分档 chips / 四正本纪详情 / 订单 6 Tab / 动态核销码 / 门店信息与核销码页 / 4 TabBar
- Web：7 菜单侧栏 / 今日概览工作台 / 三步新增商品（SKU 快捷生成+批量填充+违禁词实时校验）/ 门店营业时间+核销记录 / 会员等级分布卡
- 种子数据对齐原型：正隆记总号等 3 门店、口粮/进阶/待客/礼盒典藏四档、三号究~正隆究礼盒 6 商品
- 前端依赖对齐参考工程 qingguoji-mall（web 全量对齐；h5 工具链对齐，因仅 H5 端不引入 Taro，偏差已在 openspec change 留档）

## 账号与模拟通道

| 项 | 值 |
|---|---|
| 管理端账号 | admin / admin123（平台管理员）、manager / manager123（库管主管，用于盘点二级确认） |
| 买家登录 | 任意 11 位手机号 + 验证码 123456（模拟短信） |
| 支付 | 模拟通道（RK-01 槟榔类目报备未决），幂等与双通道确认语义已保留 |

## 合规底线（硬编码）

- 警示语「长期过量咀嚼槟榔，有害口腔健康」与「不向未成年人销售」强制展示
- 商品文案违禁词校验（功效宣传词阻断上架）
- 资金与上下架操作审计日志
- 已移除模块勿恢复：优惠券（FR-15）、商品资质（FR-10-5）、售卖区域限制、RBAC/合规中心

## 规格管理

- 工作流 schema `zlj-team`（`openspec/schemas/zlj-team/`）：change 产物按端拆分文件 —— proposal.md（共享）、api.md（接口文档，先改文档再动代码）、design/backend.md+frontend.md、tasks/backend.md+frontend.md、specs/<能力>/<端>/spec.md
- 主规格库 `openspec/specs/<能力>/<端>/spec.md`：会员 / 商品 / 购物车 / 订单 / 售后 / 库存 / 门店 / 平台 / 接口契约（backend），管理端 / H5买家端（frontend）
- 契约权威：`openspec/specs/接口契约/backend/spec.md`
- 工作流详见 `AGENTS.md`；测试文档见 `docs/测试用例.md` 与 `docs/测试报告.md`
