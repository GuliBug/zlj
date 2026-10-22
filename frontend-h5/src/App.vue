<template>
  <div class="app-shell">
    <router-view :key="$route.fullPath" />
    <TabBar v-if="$route.meta.tab" />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import TabBar from './components/TabBar.vue'
import { refreshCartCount } from './store'

onMounted(refreshCartCount)
</script>
