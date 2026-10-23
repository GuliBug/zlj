## Why

项目尚未收尾,不存在已完结的归档。以本 change 承载「会员」能力的当前基线:已实现并通过质量门禁的需求以 ADDED delta 形式挂在本 change 下,后续新需求/缺陷修复继续在本 change 内追加,项目收尾阶段再统一归档同步主规格。

## What Changes

### 后端

- 无新增改动:本 change 为「会员」能力基线,已实现内容以 ADDED delta 挂在 specs/会员/backend/spec.md。

### 前端

- 无新增改动:本 change 为「会员」能力基线,已实现内容以 ADDED delta 挂在 specs/会员/frontend/spec.md。

## Capabilities

### New Capabilities

- `会员/backend`: 会员注册登录/验证码/资料与积分账户(接口行为、字段口径、状态机、数据一致性)
- `会员/frontend`: 会员注册登录/验证码/资料与积分账户(页面清单、渲染、交互、合规文案展示)

### Modified Capabilities

(无)

## Impact

### 后端

- 无新增影响(基线能力均已实现并上线联调)。

### 前端

- 无新增影响(基线能力均已实现并上线联调)。

## 非目标

- 不改动任何代码;本 change 仅承载能力基线与后续增量需求。
- 不重构主规格库能力目录结构。

## 修订记录

| 日期 | 摘要 | 提出方 |
| --- | --- | --- |
| 2026-10-23 | 建立「会员」能力基线 change(由已完成归档内容收敛而来) | 用户 |
