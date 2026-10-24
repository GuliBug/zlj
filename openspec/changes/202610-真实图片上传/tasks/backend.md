# tasks/backend.md — 真实图片上传

## 商品 / 售后模块

- [x] T-B1 `Product.images` / `Product.detailImages` 加 `@Lob`（CLOB/LONGTEXT）【FR-10-1｜验证：编译 + E2E】
- [x] T-B2 `AfterSale.images` 加 `@Lob`【FR-06-2｜验证：编译 + E2E】
- [x] T-B3 质量门禁：`cd backend && ./mvnw.cmd -q compile` 零报错【验证：构建】
- [x] T-B4 重启后端后 E2E 全量回归（`node tests/e2e/e2e-mall.mjs` 146/146 PASS）【验证：E2E】
- [x] T-B6（BUG-10）多图分隔符 `,` → `|`：ProductService join/split、AfterSaleService join/split、OrderService.firstImage、CartService.first、DemoDataInitializer 种子图【FR-06-2/FR-10-1｜验证：UI E2E UC-21 凭证回显 dataURL】

## 契约同步

- [ ] T-B5 归档时将图片字段语义与 `|` 分隔符口径同步进主规格库「接口契约」能力【验证：归档检查】
