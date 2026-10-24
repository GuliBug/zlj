# 真实图片上传

## Why

当前图片链路为「伪上传」：H5 售后凭证以「勾选占位」代替上传（提交时传 `凭证占位1` 字符串），Web 管理端商品主图/详情图为手填 URL 文本框（示意图）。真实流程走不通：商家无法上传实拍图，买家无法提交问题凭证截图。本期将双端图片上传改为真实文件选择，前端压缩为 base64（dataURL）随表单提交，后端以 CLOB/TEXT 存储并原样回显，打通「上传→存储→回显」闭环。

## What Changes

### 后端

- `Product.images` / `Product.detailImages` / `AfterSale.images` 三个图片列改为大文本存储（`@Lob`，H2=CLOB / MySQL=LONGTEXT），接口字段结构不变；字段值从「URL 逗号串」扩展为「URL 或 dataURL 逗号串」（base64 字符集不含逗号，逗号拼接与拆分逻辑不变）。

### 前端

#### H5（frontend-h5）

- `AftersaleNew.vue`：凭证位改为真实上传——点击空位唤起系统相册/文件选择（`input[type=file] accept=image/*`），选中后 canvas 压缩为 base64 dataURL 存入凭证列表（1-3 张），已传位显示缩略图、点击删除；质量问题未传凭证前端拦截。
- `AftersaleDetail.vue`：新增「凭证」区块，回显上传的凭证缩略图（dataURL 直接渲染）。
- 新增 `src/image.js` 压缩工具（最长边 1080、JPEG 0.72）。

#### Web 管理端（frontend-web）

- `product/edit.vue`：主图/详情图的手填 URL 输入框替换为真实上传（点击缩略位选文件，主图≤3 张、详情图≤6 张，可删除），已有 URL 值回显为缩略图；新增 `utils/image.js`（同一压缩策略）。
- 售后详情抽屉与商品图片渲染已按 `src` 直出，dataURL 无需适配。

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `商品/backend`：FR-10-1 图片字段接受 dataURL 并以大文本存储。
- `商品/frontend`：管理端新增/编辑商品支持真实图片上传（主图≤3、详情图≤6，缩略回显+删除）。
- `售后/backend`：FR-06-2 凭证图片为真实 dataURL 存储（大文本）。
- `售后/frontend`：H5 售后申请真实上传凭证；售后详情展示凭证缩略图。

## Impact

### 后端

- 影响：`mall/domain/Product.java`、`mall/domain/AfterSale.java`（列类型注解）；H2 内存库每次启动重建 schema，无需迁移脚本；生产 MySQL 由 Hibernate 映射为 LONGTEXT。

### 前端

- 影响：`frontend-h5/src/pages/AftersaleNew.vue`、`AftersaleDetail.vue`、`src/image.js`（新增）；`frontend-web/src/views/product/edit.vue`、`src/utils/image.js`（新增）；测试脚本 `tests/e2e/ui-e2e.mjs`（UC-20/21 改真实上传并断言 dataURL 凭证）、`tests/e2e/ui-e2e-web.mjs`（WC-20 上传真实图片并断言 images[0] 为 dataURL）。

## 非目标

- 不引入文件服务器/OSS 与独立上传接口（仍随表单 base64 提交；后续接入 OSS 时仅替换通道，字段语义不变）。
- 不改种子数据（`/img/*.jpg` 占位图保留，前端已有兜底渲染）。
- 不做图片鉴黄/审核链路。

## 修订记录

| 日期 | 摘要 | 提出方 |
|---|---|---|
| 2026-10-24 | 初稿定稿 | 用户 |
| 2026-10-24 | E2E 首轮暴露 BUG-10：dataURL 前缀含逗号，逗号分隔劈断图片串；多图分隔符改 `|` 并全链路回归通过 | AI |
