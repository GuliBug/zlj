# api.md — 图片字段语义扩展（真实上传）

> 接口路径与字段结构无变化；仅图片类字段值的语义从「URL 串」扩展为「URL 或 dataURL 串」，存储列改为大文本。状态：已定稿（前端已按此实现）。

## 涉及字段

| 接口 | 字段 | 修正后语义 |
|---|---|---|
| POST /admin-api/products（新增）/ PUT /admin-api/products/{id}（编辑） | images / detailImages | string[]，每项为图片 URL 或 `data:image/jpeg;base64,…`（dataURL）；后端以 `|` 拼接存 CLOB/TEXT，回显按 `|` 拆分（BUG-10 修订：原逗号分隔会劈断 dataURL）。数组长度：主图≤3、详情图≤6（前端约束） |
| POST /api/aftersales（售后申请） | images | string[]，1-3 项，每项为凭证 URL 或 dataURL；质量问题必填校验不变 |
| GET /api/aftersales/{no} 等回显接口 | images / detailImages | 原样返回存储值（URL 或 dataURL），前端直接作为 img src |

## 成功示例（售后申请，真实凭证）

```json
// 请求
{
  "orderNo": "20261024151704109070",
  "type": "RETURN_REFUND",
  "reason": "不想要了",
  "description": "包装完好未拆封",
  "images": ["data:image/jpeg;base64,/9j/4AAQSkZJRgABAQ…"]
}
// 响应
{ "code": 200, "msg": "操作成功", "data": { "afterSaleNo": "AS202610241517349071" } }
// GET /api/aftersales/AS… 的 data.images[0] 以 data:image 开头（真实凭证闭环）
```

## 存储口径

- 图片串列由 VARCHAR(255) 扩为大文本：H2=CLOB，MySQL=LONGTEXT（`@Lob` 映射）；单张压缩后约 100-300KB，3 张内请求体可控。
- 多图分隔符为 `|`（BUG-10 修订）：dataURL 的 `;base64,` 前缀含逗号，逗号分隔会把 dataURL 劈断；`|` 在 base64 字符集与 dataURL 前缀中均不出现，安全。
