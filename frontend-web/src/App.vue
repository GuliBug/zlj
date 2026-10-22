<template>
  <router-view />
</template>

<script setup>
</script>

<style>
/* 红金国潮主题（对齐 Web 管理端交互原型） */
:root {
  --primary: #8f1d22;
  --primary-light: #faeee6;
  --bg: #f4f1ea;
  --card: #fffdf8;
  --text: #2b2320;
  --text-2: #8a8074;
  --line: #e8dfd2;
  --side: #241d18;
  --side-text: #a89a86;
  --green: #2e7d32;
  --orange: #b26a00;
  --gold: #c9a063;
  --radius: 10px;

  /* Element Plus 主色覆盖：对齐原型 --primary:#8f1d22（旧 #B01F24 已废弃） */
  --el-color-primary: #8f1d22;
  --el-color-primary-light-3: #a94a4e;
  --el-color-primary-light-5: #c17e81;
  --el-color-primary-light-7: #dcb3b4;
  --el-color-primary-light-8: #eacccd;
  --el-color-primary-light-9: #faeee6;
  --el-color-primary-dark-2: #72171b;
}

html,
body,
#app {
  height: 100%;
  margin: 0;
  padding: 0;
  font-family: -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: var(--bg);
  color: var(--text);
  font-size: 13px;
}

.el-menu {
  border-right: none;
}

.page-card {
  margin: 0;
}

.panel-card {
  background: var(--card);
  border-radius: var(--radius);
  border: 1px solid var(--line);
  padding: 16px;
  margin-bottom: 16px;
}

.panel-card h3.panel-title {
  font-size: 14px;
  margin: 0 0 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-title .r {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

.filter-bar {
  margin-bottom: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.overdue-row {
  --el-table-tr-bg-color: #fdf3e7;
}

/* 原型 timeline-note 样式 */
.timeline-note {
  font-size: 11px;
  color: var(--text-2);
  background: #fafbfc;
  border: 1px dashed var(--line);
  border-radius: 6px;
  padding: 8px 12px;
  margin-top: 10px;
  line-height: 1.8;
}

/* 表格行内副行说明文字 */
.sub {
  color: var(--text-2);
  font-size: 11px;
}

/* 表格背景与卡片对齐米白 */
.el-table {
  --el-table-bg-color: var(--card);
  --el-table-tr-bg-color: var(--card);
  --el-table-header-bg-color: #faf6ee;
  --el-table-border-color: var(--line);
  --el-table-row-hover-bg-color: #faf6ee;
}

.el-card {
  --el-card-border-color: var(--line);
  --el-card-border-radius: var(--radius);
  --el-card-bg-color: var(--card);
}
</style>
