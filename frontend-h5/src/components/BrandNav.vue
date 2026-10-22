<template>
  <div class="brand-navbar">
    <div class="brand-row">
      <div class="brand-seal" :style="compact ? { width: '34px', height: '34px', fontSize: '13px' } : null">隆記</div>
      <div class="brand-t">
        <h1 :style="compact ? { fontSize: '16px' } : null">正隆記</h1>
        <p v-if="!compact">正宗湘潭门店究头 · 始于一九一六</p>
      </div>
      <div class="store-chip" @click="openStores = true">🏪 {{ currentStoreName || '选择门店' }} ▾</div>
    </div>
    <div v-if="!compact" class="brand-strip">
      <span>以正立本</span><span>以究待客</span><span>正品 · 正源 · 正工 · 正心</span>
    </div>

    <!-- 门店切换弹层 -->
    <div class="mask" v-if="openStores" @click="openStores = false">
      <div class="sheet" style="padding-bottom:24px" @click.stop>
        <span class="sheet-close" @click="openStores = false">✕</span>
        <div style="font-family:var(--kai);font-size:15px;letter-spacing:1px;margin-bottom:6px">选择门店</div>
        <div
v-for="s in stores" :key="s.id" class="store-card" :style="{ opacity: s.status !== 'OPEN' ? .5 : 1 }"
          @click="pick(s)">
          <div class="s-ico">店</div>
          <div class="s-t">
            <b>{{ s.name }}<template v-if="store.selectedStoreId === s.id">（当前）</template></b>
            {{ s.address }}<br>☎ {{ s.phone }}
          </div>
          <span :style="{ color: store.selectedStoreId === s.id ? 'var(--brand)' : '#cfc4b0' }">
            {{ store.selectedStoreId === s.id ? '✓' : '›' }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { api } from '../api'
import { store, selectStore } from '../store'
import { showToast } from '../ui'

defineProps({ compact: Boolean })
const openStores = ref(false)
const stores = ref([])
const currentStoreName = computed(() => {
  const s = stores.value.find(x => x.id === store.selectedStoreId)
  return s ? s.name : ''
})
function pick(s) {
  if (s.status !== 'OPEN') { showToast('该门店休息中'); return }
  selectStore(s.id, s.name)
  openStores.value = false
  showToast('已切换门店：' + s.name + '（商品 / 核销跟随门店）')
}
onMounted(async () => {
  try {
    stores.value = await api.stores()
    if (!store.selectedStoreId && stores.value.length) {
      const open = stores.value.find(s => s.status === 'OPEN') || stores.value[0]
      selectStore(open.id, open.name)
    }
  } catch { /* 忽略 */ }
})
</script>
