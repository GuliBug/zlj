<template>
  <div>
    <div class="navbar-plain"><button class="back" @click="goBack">‹</button><div class="title">门店信息</div></div>

    <div class="card">
      <h3 style="font-family:var(--kai);font-size:15px;margin-bottom:10px;letter-spacing:1px">选择店铺（切换后商品/核销跟随门店）</h3>
      <div v-for="s in rows" :key="s.id" class="store-card" :style="{ opacity: s.status !== 'OPEN' ? .55 : 1 }" @click="pick(s)">
        <div class="s-ico">店</div>
        <div class="s-t">
          <b>{{ s.name }}<template v-if="store.selectedStoreId === s.id">（当前）</template></b>
          {{ s.address }}<br>
          ☎ {{ s.phone }}<template v-if="storeHours(s)"> · 营业 {{ storeHours(s) }}</template><template v-if="s.status !== 'OPEN'"> · 休息中</template>
        </div>
        <span :style="{ color: store.selectedStoreId === s.id ? 'var(--brand)' : '#cfc4b0' }">
          {{ store.selectedStoreId === s.id ? '✓' : '›' }}
        </span>
      </div>
    </div>
    <div class="minor" style="margin-bottom:20px">门店数据由运营后台「门店管理」维护</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'
import { store, selectStore } from '../store'
import { showToast } from '../ui'

const router = useRouter()
const rows = ref([])

function storeHours(s) {
  return s.operatingHours || s.businessHours || s.openHours || ''
}
function goBack() {
  if (window.history.length > 1) router.back()
  else router.replace('/member')
}
function pick(s) {
  if (s.status !== 'OPEN') { showToast('该门店休息中，暂不可选'); return }
  selectStore(s.id, s.name)
  showToast('已切换门店：' + s.name + '（商品 / 核销跟随门店）')
}

onMounted(async () => {
  rows.value = (await api.stores()) || []
  if (!store.selectedStoreId && rows.value.length) {
    const open = rows.value.find(s => s.status === 'OPEN') || rows.value[0]
    selectStore(open.id, open.name)
  }
})
</script>
