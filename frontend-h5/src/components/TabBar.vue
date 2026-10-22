<template>
  <div class="tabbar">
    <router-link v-for="t in tabs" :key="t.path" :to="t.path" class="tab-item" :class="{ on: isActive(t) }">
      <span class="tab-badge" v-if="t.badge && store.cartCount > 0">{{ store.cartCount > 99 ? '99+' : store.cartCount }}</span>
      <span class="tab-icon">{{ t.icon }}</span>
      <span>{{ t.text }}</span>
    </router-link>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { store } from '../store'
import pagesConfig from '../pages.json'

const route = useRoute()
// 底部导航项来自 pages.json 的 tabBar.list（与 window 主题同源）
const tabs = pagesConfig.tabBar.list.map((t) => ({
  path: t.pagePath,
  icon: t.icon,
  text: t.text,
  badge: t.badge === 'cart'
}))
function isActive(t) {
  return t.path === '/' ? route.path === '/' : route.path.startsWith(t.path)
}
</script>

<style scoped>
.tabbar {
  position: fixed; bottom: 0; left: 50%; transform: translateX(-50%);
  width: 100%; max-width: 480px; z-index: 70;
  background: #FFFDF8; border-top: 1px solid #EDE4D4;
  display: flex; padding-bottom: env(safe-area-inset-bottom);
}
.tab-item {
  flex: 1; text-align: center; padding: 6px 0 5px;
  font-size: 11px; color: #9A8B7A; position: relative;
}
.tab-item.on { color: var(--red); }
.tab-icon { display: block; font-size: 20px; line-height: 26px; filter: grayscale(.7); opacity: .75; }
.tab-item.on .tab-icon { filter: none; opacity: 1; }
.tab-badge {
  position: absolute; top: 0; right: calc(50% - 20px);
  min-width: 16px; height: 16px; line-height: 16px; padding: 0 4px;
  background: var(--red); color: #fff; border-radius: 8px; font-size: 10px;
}
</style>
