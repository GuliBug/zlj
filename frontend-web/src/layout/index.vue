<template>
  <el-container class="zl-layout">
    <!-- 侧边栏：深色 #241d18，菜单选中 #8f1d22 -->
    <el-aside width="210px" class="zl-sidebar">
      <div class="logo">正隆記 <small>门店运营后台</small></div>
      <el-menu :default-active="activeMenu" router class="side-menu">
        <el-menu-item index="/dashboard"><span class="m-ico">🏠</span>工作台</el-menu-item>
        <el-sub-menu index="goods">
          <template #title><span class="m-ico">📦</span>商品管理</template>
          <el-menu-item index="/products">商品列表</el-menu-item>
          <el-menu-item index="/products/new">新增商品</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/stock"><span class="m-ico">📊</span>库存管理</el-menu-item>
        <el-menu-item index="/stores"><span class="m-ico">🏪</span>门店管理</el-menu-item>
        <el-menu-item index="/orders"><span class="m-ico">🧾</span>订单管理</el-menu-item>
        <el-menu-item index="/aftersales"><span class="m-ico">↩️</span>售后管理</el-menu-item>
        <el-menu-item index="/members"><span class="m-ico">👥</span>会员管理</el-menu-item>
      </el-menu>
      <div class="side-warn">资损监控：重复支付 / 金额异常 / 状态回跳 / 对账差异超 24h → 告警</div>
    </el-aside>

    <el-container class="main-col">
      <el-header class="zl-header">
        <div class="crumb">{{ breadcrumb }}</div>
        <div class="role">
          <div class="avatar">掌</div>
          <span>超级管理员 · 陈掌柜（正隆记总号）</span>
          <el-divider direction="vertical" />
          <el-button link size="small" @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="zl-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import useUserStore from '@/store/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const activeMenu = computed(() => {
  const p = route.path
  if (p === '/products/new') return '/products/new'
  if (p.startsWith('/products/')) return '/products'
  return p
})

const breadcrumb = computed(() => route.meta.breadcrumb || route.meta.title || '')

function handleLogout() {
  ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
    .then(() => {
      user.logout()
      router.push('/login')
    })
    .catch(() => {})
}
</script>

<style scoped>
.zl-layout {
  height: 100%;
}
.zl-sidebar {
  background: var(--side);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.logo {
  height: 56px;
  flex: none;
  display: flex;
  align-items: center;
  padding: 0 18px;
  color: #f3e2c0;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 3px;
  border-bottom: 1px solid #3a2f24;
  font-family: KaiTi, 'STKaiti', serif;
}
.logo small {
  font-size: 10px;
  color: var(--side-text);
  font-weight: 400;
  margin-left: 8px;
  letter-spacing: 0;
  font-family: sans-serif;
}
.side-menu {
  flex: 1;
  overflow-y: auto;
  background: var(--side);
  border-right: none;
}
.side-menu :deep(.el-menu-item),
.side-menu :deep(.el-sub-menu__title) {
  color: var(--side-text);
  background: transparent;
}
.side-menu :deep(.el-menu-item:hover),
.side-menu :deep(.el-sub-menu__title:hover) {
  background: #2b231d;
  color: #fff;
}
.side-menu :deep(.el-menu-item.is-active) {
  background: var(--primary);
  color: #fff;
}
.side-menu :deep(.el-sub-menu .el-menu) {
  background: transparent;
}
.side-menu :deep(.el-sub-menu .el-menu .el-menu-item) {
  padding-left: 44px !important;
  font-size: 12px;
}
.m-ico {
  margin-right: 8px;
}
.side-warn {
  flex: none;
  padding: 12px 16px;
  font-size: 11px;
  color: #d9a05b;
  border-top: 1px solid #3a2f24;
  line-height: 1.6;
}
.main-col {
  min-width: 0;
}
.zl-header {
  height: 56px;
  background: var(--card);
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: center;
  padding: 0 20px;
  gap: 12px;
}
.crumb {
  flex: 1;
  font-size: 14px;
  font-weight: 600;
}
.role {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-2);
  font-size: 13px;
}
.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}
.zl-main {
  background: var(--bg);
  padding: 18px;
  overflow-y: auto;
}
</style>
