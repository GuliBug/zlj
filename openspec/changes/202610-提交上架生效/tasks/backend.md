# tasks/backend.md — 提交上架生效

## 商品模块

- [x] T-B1 `ProductService.create()`：`product.setStatus("OFF")` 改为按 `body.status()` 解析（仅 `"ON"` 上架，其余落 `OFF`）；同时补全 `ProductBody` record 缺失的 `status` 字段（根因之一：DTO 层丢失前端传入的 status）【FR-10-1｜验证：E2E 接口级回归】
- [x] T-B2 质量门禁：`cd backend && ./mvnw.cmd -q compile` 零报错【验证：构建】
- [x] T-B3 重启后端后 E2E 全量回归（`node tests/e2e/e2e-mall.mjs` 146/146 PASS）【验证：E2E】

## 契约同步

- [ ] T-B4 归档时将 api.md 中 `POST /admin-api/products` 的 status 字段语义同步进主规格库「接口契约」能力【验证：归档检查】
