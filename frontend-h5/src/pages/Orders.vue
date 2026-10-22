<template>
  <div>
    <div class="navbar-plain"><button class="back" @click="goBack">‹</button><div class="title">我的订单</div></div>

    <!-- 6 个状态 Tab（映射接口状态） -->
    <div class="otabs">
      <div v-for="t in tabs" :key="t.key" class="otab" :class="{ on: curTab === t.key }" @click="switchTab(t.key)">
        {{ t.text }}
      </div>
    </div>

    <div v-if="rows.length">
      <div v-for="o in rows" :key="o.orderNo" class="order-card" @click="$router.push('/order/' + o.orderNo)">
        <div class="order-top">
          <span style="min-width:0;overflow:hidden;text-overflow:ellipsis">订单号 {{ o.orderNo }} · {{ o.fulfillmentType === 'PICKUP' ? '到店自提' : '快递配送' }}</span>
          <span class="status">{{ o.statusText }}</span>
        </div>
        <div v-for="(it, i) in (o.items || [])" :key="i" class="order-goods" style="margin-bottom:8px">
          <GoodsMedia :image="it.image" :name="it.name" height="48px" char-size="22px" style="width:48px;border-radius:8px;flex:none" class="thumb" />
          <div style="flex:1;min-width:0;font-size:13px;font-family:var(--kai)">
            {{ it.name }}<br>
            <span style="font-size:11px;color:var(--text-2);font-family:sans-serif">{{ it.flavor }} / {{ it.spec }} × {{ it.quantity }}</span>
          </div>
          <span class="price" style="font-size:14px"><small>¥</small>{{ yuan((it.price || 0) * (it.quantity || 0)) }}</span>
        </div>
        <div class="order-bot">
          <span style="font-size:11px;color:var(--text-2)">实付 ¥{{ yuan(o.payAmount) }}</span>
          <div style="display:flex;gap:8px" @click.stop>
            <template v-if="o.status === 'WAIT_PAY'">
              <button class="btn-mini" @click="cancel(o)">取消订单</button>
              <button class="btn-mini pri" @click="pay(o)">去支付</button>
            </template>
            <button v-if="o.status === 'PICKUP_WAIT'" class="btn-mini pri" @click="$router.push('/verify-code?orderId=' + o.orderNo)">出示核销码</button>
            <button v-if="o.status === 'SHIPPED'" class="btn-mini" @click="confirmRecv(o)">确认收货</button>
            <button v-if="o.status === 'COMPLETED' || o.status === 'AFTER_SALE'" class="btn-mini pri" @click="$router.push('/aftersale/new/' + o.orderNo)">申请售后</button>
          </div>
        </div>
      </div>
      <div class="muted load-more" v-if="hasMore" @click="load(false)">加载更多</div>
    </div>
    <div class="result" v-else-if="loaded" style="padding-top:100px"><p>该状态下暂无订单</p></div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, yuan } from '../api'
import { showToast, showConfirm } from '../ui'
import GoodsMedia from '../components/GoodsMedia.vue'

const route = useRoute()
const router = useRouter()
const tabs = [
  { key: '', text: '全部' },
  { key: 'WAIT_PAY', text: '待支付' },
  { key: 'PAID', text: '待发货' },
  { key: 'PICKUP_WAIT', text: '待自提' },
  { key: 'COMPLETED', text: '已完成' },
  { key: 'AFTER_SALE', text: '售后' }
]
const curTab = ref(route.query.tab || '')
const rows = ref([])
const page = ref(1)
const hasMore = ref(false)
const loaded = ref(false)

function goBack() {
  if (window.history.length > 1) router.back()
  else router.replace('/member')
}
function switchTab(k) { curTab.value = k; load(true) }

async function load(reset) {
  if (reset) { page.value = 1; rows.value = [] }
  const d = await api.orders({ status: curTab.value, page: page.value, size: 10 })
  rows.value = reset ? (d.rows || []) : rows.value.concat(d.rows || [])
  hasMore.value = rows.value.length < d.total
  page.value++
  loaded.value = true
}
async function cancel(o) {
  const ok = await showConfirm('取消订单', '取消后订单将关闭并释放库存，确定取消？')
  if (!ok) return
  await api.cancelOrder(o.orderNo)
  showToast('订单已取消')
  load(true)
}
function pay(o) {
  router.push(`/pay-result/${o.orderNo}?pay=1`)
}
async function confirmRecv(o) {
  const ok = await showConfirm('确认收货', '请确认已收到商品且无破损，确认后订单完成。')
  if (!ok) return
  await api.confirmOrder(o.orderNo)
  showToast('确认收货成功')
  load(true)
}

onMounted(load)
</script>

<style scoped>
.load-more { text-align: center; padding: 12px 0; }
</style>
