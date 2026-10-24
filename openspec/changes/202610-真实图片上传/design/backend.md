# design/backend.md — 真实图片上传

### BD-01 图片列改为 @Lob 大文本，不做迁移脚本

- 结论：`Product.images` / `Product.detailImages` / `AfterSale.images` 增加 `@Lob`（Hibernate 6：H2 映射 CLOB，MySQL 映射 LONGTEXT）。
- 取舍：开发库为 H2 内存库、每次启动重建 schema（ddl-auto:update 首建生效），无需 ALTER 迁移；生产 MySQL 首次建表即 LONGTEXT。
- 否决的替代方案：独立文件上传接口 + OSS——本期否决（PRD 未要求，base64 随表单提交即可闭环；后续接入时仅替换前端通道，字段语义不变）。

### BD-02 存储分隔符为 `|`（BUG-10 修订：原逗号方案被证伪）

- 初版假设「base64 字符集不含逗号，逗号拼接零改动」是错的：dataURL 的 `;base64,` 前缀本身含逗号，后端 `split(",")` 会把 dataURL 劈成两段（E2E UC-21 首轮跑出：详情页凭证回显为两张 SVG 占位图，即 dataURL 被劈开后触发全局 img error 兜底）。
- 修订：多图分隔符统一改为 `|`（base64 字符集与 dataURL 前缀均不含 `|`）；涉及 `ProductService`（join/split）、`AfterSaleService`（join/split）、`OrderService.firstImage`、`CartService.first`、`DemoDataInitializer` 种子图。
- 接口出参仍为 string[]，前端无感知；H2 内存库重启即重建，无历史数据迁移。

## 修订记录

| 日期 | 摘要 | 提出方 |
|---|---|---|
| 2026-10-24 | 初稿定稿 | 用户 |
| 2026-10-24 | BUG-10：分隔符 `,` → `|`（dataURL 前缀含逗号，逗号方案把 dataURL 劈断） | AI（E2E UC-21 暴露） |
