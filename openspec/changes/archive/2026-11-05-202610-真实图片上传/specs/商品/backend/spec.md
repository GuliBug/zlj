# 商品能力 delta — 真实图片上传

## MODIFIED Requirements

### Requirement: FR-10-1 商品新增
管理端 SHALL 支持新增商品：名称/二级分类/品牌/产地/描述/SKU 列表（口味、规格、条码、售价、成本价、库存）/主图与详情图；SKU 支持按口味×规格笛卡尔积快捷生成与批量填充；提交校验条码唯一、售价>0、库存≥0。请求 `status` 字段 SHALL 生效：`ON` 为提交上架（买家端即时可见），`OFF` 为保存草稿；缺省或非法值按 `OFF`（草稿）处理。主图/详情图 SHALL 支持真实文件上传，前端压缩为 base64 dataURL 提交，后端以大文本（CLOB/LONGTEXT）存储并原样回显。

#### Scenario: SKU 笛卡尔积生成
- **WHEN** 录入口味[原味,枸杞]与规格[10元,20元]并点击生成
- **THEN** 自动生成 4 条 SKU 行

#### Scenario: 提交上架即上架（BUG-09 回归）
- **WHEN** 管理端填写完整商品信息并以「提交上架」（status=ON）保存
- **THEN** 接口返回 id，且该商品状态为 `ON`，买家端商品列表即时可见
- **WHEN** 新增商品时 status 传非法值或缺省
- **THEN** 商品状态落为 `OFF`（草稿），买家端不可见

#### Scenario: 真实上传主图
- **WHEN** 管理端在第③步点击上传位选择本地图片文件
- **THEN** 图片经前端压缩后以 dataURL 进入 images 列表并显示缩略图，提交后详情/买家端以同一 dataURL 回显
