# 售后能力 delta（frontend）— 真实图片上传

## ADDED Requirements

### Requirement: FR-06-2 售后凭证上传（H5）
售后申请页 SHALL 提供 1-3 个凭证位：点击空位唤起系统文件选择（accept=image/*），选中图片压缩为 dataURL 后即时缩略预览；已传位 SHALL 可删除；原因为「质量问题」且未传凭证时 SHALL 前端拦截提交。售后详情页 SHALL 新增凭证区块，回显已上传凭证缩略图（无凭证时显示占位文案）。

#### Scenario: 选择图片即时预览
- **WHEN** 买家点击凭证空位并选择一张本地图片
- **THEN** 该凭证位显示压缩后的缩略图，可再次点击删除

#### Scenario: 详情页凭证回显
- **WHEN** 买家进入含凭证的售后详情页
- **THEN** 凭证区块渲染已上传的凭证缩略图
