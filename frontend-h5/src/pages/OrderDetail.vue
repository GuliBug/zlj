<template>
  <div class="has-bottom-bar" v-if="d">
    <div class="navbar-plain"><button class="back" @click="goBack">‹</button><div class="title">订单详情</div></div>

    <!-- 订单进度时间轴 -->
    <div class="card">
      <div class="card-title">订单进度 <span class="tag">{{ d.statusText }}</span></div>
      <div class="timeline" v-if="d.timeline && d.timeline.length">
        <div v-for="(t, i) in d.timeline" :key="i" class="timeline-item">
          <div class="t-action">{{ t.action }}</div>
          <div class="t-time">{{ t.time }}</div>
          <div class="t-remark" v-if="t.remark">{{ t.remark }}</div>
        </div>
      </div>
      <div class="muted" v-else>暂无流转记录</div>
    </div>

    <!-- 动态核销码（待自提）：对齐 hq-card -->
    <div class="hq-card" v-if="d.status === 'PICKUP_WAIT'">
      <div style="font-family:var(--kai);font-size:16px;letter-spacing:2px">到店出示 · 掌柜扫码核销</div>
      <div style="font-size:11px;color:var(--text-2);margin-top:4px" v-if="pickupStore">当前门店：<b>{{ pickupStore.name }}</b></div>
      <div class="v-code">{{ vCode || '······' }}</div>
      <div class="muted" style="text-align:center;font-size:11px">
        {{ countdown > 0 ? countdown + ' 秒后自动刷新，防截图盗用' : '刷新中…' }}
      </div>
      <div class="barcode"></div>
      <div style="font-size:10px;color:var(--text-2);margin-top:10px;line-height:1.7">
        · 核销码每分钟自动刷新，防截图盗用<br>· 到店出示此码给掌柜核销提货
      </div>
    </div>

    <!-- 商品清单 -->
    <div class="card">
      <div class="card-title">商品清单</div>
      <div v-for="(it, i) in (d.items || [])" :key="i" class="o-item">
        <GoodsMedia :image="it.image" :name="it.name" height="52px" char-size="24px" style="width:52px;border-radius:8px;flex:none" />
        <div style="flex:1;min-width:0;font-size:13px;font-family:var(--kai)">
          {{ it.name }}<br>
          <span style="font-size:11px;color:var(--text-2);font-family:sans-serif">{{ it.flavor }} / {{ it.spec }}</span>
        </div>
        <div style="text-align:right">
          <div class="price" style="font-size:14px"><small>¥</small>{{ yuan(it.price) }}</div>
          <div class="muted">x{{ it.quantity }}</div>
        </div>
      </div>
    </div>

    <!-- 自提门店 / 收货信息 -->
    <div class="card" v-if="d.fulfillmentType === 'PICKUP'">
      <div class="card-title">自提门店</div>
      <template v-if="pickupStore">
        <div style="font-weight:600;font-family:var(--kai)">{{ pickupStore.name }}</div>
        <div class="muted" style="margin-top:6px">{{ pickupStore.address }}</div>
        <div class="muted" v-if="pickupStore.phone">☎ {{ pickupStore.phone }} · 到店报手机尾号核销</div>
      </template>
      <div class="muted" v-else>—</div>
    </div>
    <div class="card" v-else>
      <div class="card-title">收货信息</div>
      <template v-if="recv">
        <div>{{ recv.receiverName || recv.name || recv.receiver || '' }} {{ recv.phone || recv.receiverPhone || '' }}</div>
        <div class="muted" style="margin-top:6px">{{ recv.fullAddress || recv.addressText || recv.address || recv.region + ' ' + (recv.detail || '') }}</div>
      </template>
      <div class="muted" v-else>—</div>
    </div>

    <!-- 物流信息 -->
    <div class="card" v-if="d.logisticsCompany || d.logisticsNo">
      <div class="card-title">物流信息</div>
      <div class="settle-row"><span style="color:var(--text-2)">承运公司</span><span>{{ d.logisticsCompany }}</span></div>
      <div class="settle-row"><span style="color:var(--text-2)">运单号</span><span>{{ d.logisticsNo }}</span></div>
    </div>

    <!-- 金额明细 -->
    <div class="card">
      <div class="card-title">金额明细</div>
      <div class="settle-row"><span>商品合计</span><span>¥{{ yuan(d.goodsAmount) }}</span></div>
      <div class="settle-row"><span>运费</span><span>{{ d.freight ? '¥' + yuan(d.freight) : '免运费' }}</span></div>
      <div class="settle-row" v-if="d.pointsDeduction">
        <span>积分抵扣{{ d.pointsUsed ? '（' + d.pointsUsed + '积分）' : '' }}</span>
        <span class="price">-¥{{ yuan(d.pointsDeduction) }}</span>
      </div>
      <div class="settle-row total"><span>实付</span><span style="color:var(--brand);font-size:17px">¥{{ yuan(d.payAmount) }}</span></div>
    </div>

    <div class="minor" style="margin-bottom:60px">如有疑问可联系掌柜小二：工作日 9:00-18:00 · 400-XXX-XXXX</div>

    <!-- 吸底操作 -->
    <div class="bottom-bar" v-if="d.status === 'WAIT_PAY' || d.status === 'SHIPPED' || canAftersale || afterSaleOngoing">
      <template v-if="d.status === 'WAIT_PAY'">
        <button class="btn btn-gray" style="flex:1" @click="cancel">取消订单</button>
        <button class="btn btn-buy" style="flex:1" @click="payNow">立即支付</button>
      </template>
      <template v-else-if="d.status === 'SHIPPED'">
        <button class="btn btn-main" style="flex:1" @click="confirmRecv">确认收货</button>
      </template>
      <template v-else-if="afterSaleOngoing">
        <button class="btn btn-buy" style="flex:1" @click="$router.push('/aftersale/' + d.aftersale.afterSaleNo)">查看售后进度</button>
      </template>
      <template v-else>
        <button class="btn btn-buy" style="flex:1" @click="$router.push('/aftersale/new/' + d.orderNo)">申请售后</button>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, yuan } from '../api'
import { showConfirm, showToast } from '../ui'
import GoodsMedia from '../components/GoodsMedia.vue'

const route = useRoute()
const router = useRouter()
const d = ref(null)
const vCode = ref('')
const countdown = ref(0)
let timer = null

const pickupStore = computed(() => {
  const x = d.value
  if (!x) return null
  if (x.store) return x.store
  if (x.storeName) return { name: x.storeName, address: x.storeAddress, phone: x.storePhone }
  return null
})
const recv = computed(() => {
  const x = d.value
  if (!x) return null
  // 后端详情为平铺字段：receiver / receiverPhone / address（收货地址文本）；对象形态仅为兼容预留（BUG-11：字符串曾被当对象取属性渲染出 undefined）
  if (x.address && typeof x.address === 'object') return x.address
  if (x.receiver) return { receiver: x.receiver, phone: x.receiverPhone, addressText: typeof x.address === 'string' ? x.address : x.addressText }
  return null
})
const canAftersale = computed(() => ['COMPLETED', 'AFTER_SALE', 'SHIPPED'].includes(d.value.status))
// 售后单进行中（待审核/待寄回/退款中）：吸底主按钮改为「查看售后进度」
const afterSaleOngoing = computed(() => !!d.value.aftersale && ['APPLYING', 'WAIT_RETURN', 'REFUNDING'].includes(d.value.aftersale.status))

function goBack() {
  if (window.history.length > 1) router.back()
  else router.replace('/orders')
}
async function load() {
  d.value = await api.orderDetail(route.params.no)
}
async function refreshCode() {
  const r = await api.verifyCode(route.params.no)
  vCode.value = r.code
  countdown.value = Math.min(r.expiresInSec || 60, 60)
  clearInterval(timer)
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) refreshCode()
  }, 1000)
}
async function cancel() {
  const ok = await showConfirm('取消订单', '取消后订单将关闭并释放库存，确定取消？')
  if (!ok) return
  await api.cancelOrder(d.value.orderNo)
  showToast('订单已取消')
  load()
}
async function payNow() {
  try { await api.pay(d.value.orderNo) } catch { /* 已支付则忽略 */ }
  router.replace(`/pay-result/${d.value.orderNo}?from=detail`)
}
async function confirmRecv() {
  const ok = await showConfirm('确认收货', '请确认已收到商品且无破损，确认后订单完成。')
  if (!ok) return
  await api.confirmOrder(d.value.orderNo)
  showToast('确认收货成功')
  load()
}

onMounted(async () => {
  await load()
  if (d.value.status === 'PICKUP_WAIT') refreshCode()
})
onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.v-code {
  font-size: 40px; font-weight: 800; letter-spacing: 10px; text-align: center;
  color: var(--brand); font-family: 'Courier New', monospace; padding: 14px 0 6px;
}
.o-item { display: flex; align-items: center; gap: 10px; padding: 8px 0; }
</style>
