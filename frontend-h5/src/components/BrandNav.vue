<template>
  <div class="brand-navbar">
    <div class="brand-row">
      <div class="brand-seal" :style="compact ? { width: '34px', height: '34px', fontSize: '13px' } : null">隆記</div>
      <div class="brand-t">
        <h1 :style="compact ? { fontSize: '16px' } : null">正隆記</h1>
        <p v-if="!compact">正宗湘潭门店究头 · 始于一九一六</p>
      </div>
      <!-- 单店模式：仅展示当前门店，不支持切换 -->
      <div class="store-chip">🏪 {{ currentStoreName || '正隆记门店' }}</div>
    </div>
    <div v-if="!compact" class="brand-strip">
      <span>以正立本</span><span>以究待客</span><span>正品 · 正源 · 正工 · 正心</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { api } from '../api'
import { applyDefaultStore, store } from '../store'

defineProps({ compact: Boolean })
const stores = ref([])
const currentStoreName = computed(() => {
  const s = stores.value.find(x => x.id === store.selectedStoreId)
  return s ? s.name : store.selectedStoreName
})
onMounted(async () => {
  try {
    stores.value = await api.stores()
    applyDefaultStore(stores.value)
  } catch { /* 忽略 */ }
})
</script>
