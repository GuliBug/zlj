# design/frontend.md — 真实图片上传

## H5（frontend-h5）

### FD-01 canvas 压缩为 dataURL（src/image.js）

- 结论：`compressImage(file)`：createImageBitmap/Image + canvas 重绘，最长边压到 1080px，导出 JPEG quality 0.72 的 dataURL；失败（非图片/解码异常）返回 null 并提示。
- 取舍：前端压缩而非原图直传——控制请求体与 CLOB 体积；PNG 透明图转 JPEG 会丢透明通道，凭证场景可接受。

### FD-02 售后凭证位改为真实上传（AftersaleNew.vue）

- 结论：3 个凭证位绑定同一个隐藏 `input[type=file accept=image/*]`；点空位选图 → 压缩 → 追加（≤3）；已传位显示缩略图，点击删除。质量问题未传凭证前端直接拦截（与后端 FC-060 对齐）。
- 移除「演示环境：以勾选占位代替上传图片」文案。

### FD-03 售后详情新增凭证区块（AftersaleDetail.vue）

- 结论：工单信息卡下新增「凭证」卡，`d.images` 有值时渲染缩略图（dataURL 直接作 img src），空时显示「无凭证」。打通「上传→存储→回显」闭环。

## 管理端（frontend-web）

### FD-04 商品主图/详情图真实上传（product/edit.vue）

- 结论：手填 URL 文本框替换为上传缩略位：主图≤3 张、详情图≤6 张；隐藏 `input[type=file]`，选中即压缩追加，缩略图右上角「×」删除；已有 URL 值回显为缩略图（seed 数据 /img/*.jpg 与 dataURL 同渲染路径）。
- 取舍：不做拖拽/粘贴上传（管理端最小可用即可）；不保留 URL 手填入口（避免双入口混淆，历史 URL 仅回显）。

### 契约疑问

- 无。

## 修订记录

| 日期 | 摘要 | 提出方 |
|---|---|---|
| 2026-10-24 | 初稿定稿 | 用户 |
| 2026-10-24 | BUG-10 随修：AftersaleDetail 凭证回显兼容数组/字符串（后端分隔符改 `|`，出参数组不变） | AI（E2E UC-21 暴露） |
