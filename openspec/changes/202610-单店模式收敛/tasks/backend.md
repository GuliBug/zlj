## 1. 种子数据

- [x] 1.1 `DemoDataInitializer` 仅保留默认门店「正隆记总号」，删除另 2 家；就绪日志改为「门店 1 家（单店模式）」；验证：重启后端后 GET /api/stores 返回单元素
- [x] 1.2 收尾：`cd backend && ./mvnw -q compile` 零报错

## 2. 收尾门禁

- [x] 2.1 `node tests/e2e/e2e-mall.mjs` 全部用例 PASS（门店相关断言如引用门店数量需同步修正），更新 docs/测试用例.md 与 docs/测试报告.md
