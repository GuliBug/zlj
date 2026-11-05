# api.md — 新增商品 status 字段语义修正

> 接口路径与字段结构无变化，仅 `status` 请求字段语义从「被忽略」修正为「生效」。状态：已定稿（前端已按此实现）。

## POST /admin-api/products 新增商品

### 请求字段修正

| 字段 | 类型 | 必填 | 修正后语义 |
|---|---|---|---|
| status | string | 否 | `ON`=提交上架（买家端即时可见）；`OFF`=保存草稿（买家端不可见）；**缺省或非法值一律按 `OFF` 处理（草稿安全兜底）**。修正前该字段被服务端忽略、恒为 `OFF`。 |

其余字段（name/categoryId/brand/origin/description/images/detailImages/skus）与响应结构 `{id}` 不变，见 `openspec/specs/接口契约/backend/spec.md`。

### 成功示例（提交上架）

```json
// 请求
{
  "name": "六号究 · UI自动化新品1154",
  "categoryId": 1,
  "brand": "正隆記",
  "origin": "湖南湘潭",
  "status": "ON",
  "description": "纯描述性文案",
  "images": [],
  "detailImages": [],
  "skus": [{ "flavor": "UI究", "spec": "30g×10包", "barcode": "6901234500001", "price": 3990, "costPrice": 0, "stock": 100 }]
}

// 响应
{ "code": 200, "msg": "操作成功", "data": { "id": 12 } }
// 后续 GET /admin-api/products 返回该商品 status === "ON"（修正前为 "OFF"，即 BUG-09）
```

### 失败示例（不变）

```json
{ "code": 500, "msg": "商品名称不能为空" }
{ "code": 500, "msg": "内容命中违禁词: 提神、醒脑" }
```
