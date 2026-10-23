## ADDED Requirements

### Requirement: WA-20 商品图片加载失败回退
Web 管理端商品列表缩略图与商品编辑页图片预览 SHALL 在图片 URL 加载失败（404/资源缺失）时回退显示品牌占位图（红金印章风格），不得显示「加载失败」破图态；图片 URL 可正常加载时 SHALL 显示原图片。

#### Scenario: 商品图 404 回退
- **WHEN** 商品的 image 字段指向不存在的资源（如演示数据 `/img/zhenglongji-*.jpg`）
- **THEN** 商品列表缩略图显示品牌占位图，而非「加载失败」

#### Scenario: 无图商品占位
- **WHEN** 商品 image 字段为空
- **THEN** 缩略图直接显示品牌占位图（既有行为保持）

#### Scenario: 编辑页预览回退
- **WHEN** 商品编辑页填写的图片 URL 无法加载
- **THEN** 预览区显示品牌占位图，不影响表单其他字段编辑
