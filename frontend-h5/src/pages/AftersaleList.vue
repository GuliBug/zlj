<template>
  <div>
    <div class="navbar-plain"><button class="back" @click="goBack">‹</button><div class="title">售后记录</div></div>
    <div v-if="rows.length">
      <div v-for="a in rows" :key="a.afterSaleNo" class="order-card" @click="$router.push('/aftersale/' + a.afterSaleNo)">
        <div class="order-top">
          <span>工单 {{ a.afterSaleNo }}</span>
          <span class="status">{{ a.statusText }}</span>
        </div>
        <div style="font-size:13px">{{ a.typeText }} · 订单 {{ a.orderNo }}</div>
        <div class="order-bot">
          <span style="font-size:11px;color:var(--text-2)">{{ a.createdAt }}</span>
          <span class="price" style="font-size:14px"><small>¥</small>{{ yuan(a.refundAmount) }}</span>
        </div>
      </div>
      <div class="muted load-more" v-if="hasMore" @click="load(false)">加载更多</div>
    </div>
    <div class="result" v-else-if="loaded" style="padding-top:100px"><p>暂无售后记录，可在「我的订单」申请售后</p></div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api, yuan } from '../api'

const router = useRouter()
const rows = ref([])
const page = ref(1)
const hasMore = ref(false)
const loaded = ref(false)

function goBack() {
  if (window.history.length > 1) router.back()
  else router.replace('/member')
}
async function load(reset) {
  if (reset) { page.value = 1; rows.value = [] }
  const d = await api.aftersales({ page: page.value, size: 10 })
  rows.value = reset ? d.rows : rows.value.concat(d.rows)
  hasMore.value = rows.value.length < d.total
  page.value++
  loaded.value = true
}
onMounted(() => load(true))
</script>

<style scoped>
.load-more { text-align: center; padding: 12px 0; }
</style>
