# h5-client Specification

## ADDED Requirements

### Requirement: H5-08 页面配置单一来源
H5 工程 SHALL 以 `frontend-h5/src/pages.json` 作为页面配置唯一来源（三段式：window 主题 / pages 页面清单 / tabBar 底部导航，组织方式对齐参考工程 app.config.ts）；路由、页面标题、登录拦截与 TabBar 渲染均由该配置生成，禁止在组件内硬编码页面清单。

#### Scenario: 新增页面零散改动
- **WHEN** 在 pages.json 增加一个页面条目并放入 pages 目录
- **THEN** 路由、标题与（若声明 tab）底部导航自动生效，无需改其他代码
