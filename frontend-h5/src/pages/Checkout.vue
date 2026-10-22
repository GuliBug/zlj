<template>
  <div class="has-bottom-bar" v-if="items.length">
    <div class="navbar-plain"><button class="back" @click="goBack">‹</button><div class="title">确认订单</div></div>

    <!-- 表单态 -->
    <template v-if="phase === 'form'">
      <div class="card">
        <!-- seg 分段（原 .f-tab；UI 自动化同步改用 .seg） -->
        <div class="seg">
          <div :class="{ on: fulfillmentType === 'EXPRESS' }" @click="fulfillmentType = 'EXPRESS'">🚚 快递配送</div>
          <div :class="{ on: fulfillmentType === 'PICKUP' }" @click="fulfillmentType = 'PICKUP'">🏪 到店自提（核销）</div>
        </div>

        <!-- 地址块 -->
        <div v-if="fulfillmentType === 'EXPRESS'" class="addr" @click="pickAddress">
          <div class="ico">📍</div>
          <div class="txt" v-if="address">
            <b>{{ address.receiver }} {{ maskPhone(address.phone) }}</b>
            <span>{{ address.region }} {{ address.detail }}<template v-if="address.isDefault">（默认）</template></span>
          </div>
          <div class="txt" v-else><b style="color:var(--text-2)">点击新增收货地址</b><span>暂无收货地址，请先填写</span></div>
          <span style="color:#cfc4b0">›</span>
        </div>

        <!-- 自提门店块 -->
        <div v-else class="addr" style="cursor:default" @click="showStoreSheet = true">
          <div class="ico">🏪</div>
          <div class="txt">
            <b>{{ pickupStore ? pickupStore.name : '选择自提门店' }}</b>
            <span v-if="pickupStore">{{ pickupStore.address }} · 到店报手机尾号核销</span>
            <span v-else>点击选择门店</span>
          </div>
          <span style="color:#cfc4b0">›</span>
        </div>
      </div>

      <!-- 商品清单卡 -->
      <div class="card">
        <div v-for="(it, i) in items" :key="i" class="o-item">
          <GoodsMedia :image="it.image" :name="it.name" height="44px" char-size="18px" style="width:44px;border-radius:8px;flex:none" />
          <div style="flex:1;min-width:0;font-size:12px">
            {{ it.name }}<br>
            <span style="color:var(--text-2)">{{ it.flavor }} / {{ it.spec }} × {{ it.quantity }}</span>
          </div>
          <span class="price" style="font-size:14px"><small>¥</small>{{ yuan(it.price * it.quantity) }}</span>
        </div>
      </div>

      <!-- 积分抵现（接口下单必传字段） -->
      <div class="card" v-if="member" style="display:flex;justify-content:space-between;align-items:center">
        <div>
          <div style="font-size:13px">积分抵现</div>
          <div class="muted" style="font-size:11px">可用 {{ member.points }} 积分 · 100 积分 = 1 元，上限实付 5%</div>
        </div>
        <span class="checkbox" :class="{ on: usePoints }" style="width:18px;height:18px" @click="usePoints = !usePoints">{{ usePoints ? '✓' : '' }}</span>
      </div>

      <!-- 金额明细卡（预估） -->
      <div class="card">
        <div class="settle-row"><span>商品合计</span><span>¥{{ yuan(goodsAmount) }}</span></div>
        <div class="settle-row"><span>运费</span><span>{{ fulfillmentType === 'PICKUP' ? '到店自提 免运费' : (goodsAmount >= 9900 ? '包邮' : '¥6.00') }}</span></div>
        <div class="settle-row total"><span>应付</span><span style="color:var(--brand)">¥{{ yuan(goodsAmount + (fulfillmentType === 'PICKUP' || goodsAmount >= 9900 ? 0 : 600)) }}</span></div>
        <div style="font-size:11px;color:var(--text-2);margin-top:8px">最终金额以下单后服务端计算为准</div>
      </div>

      <div class="minor" style="margin-bottom:90px">提交订单即表示已知晓：本商品不向未成年人销售</div>
    </template>

    <!-- 金额明细态（订单已提交，服务端返回值） -->
    <template v-else>
      <div class="card">
        <div style="font-family:var(--kai);font-size:16px;letter-spacing:2px;margin-bottom:6px">订单已提交</div>
        <div class="muted" style="font-size:12px">订单号：{{ orderInfo.orderNo }}</div>
        <div style="margin-top:10px">
          <div class="settle-row"><span>商品合计</span><span>¥{{ yuan(orderInfo.goodsAmount) }}</span></div>
          <div class="settle-row"><span>运费</span><span>{{ orderInfo.freight ? '¥' + yuan(orderInfo.freight) : '免运费' }}</span></div>
          <div class="settle-row" v-if="orderInfo.pointsDeduction">
            <span>积分抵扣（{{ orderInfo.pointsUsed }}积分）</span><span class="price">-¥{{ yuan(orderInfo.pointsDeduction) }}</span>
          </div>
          <div class="settle-row total"><span>应付</span><span style="color:var(--brand);font-size:17px">¥{{ yuan(orderInfo.payAmount) }}</span></div>
        </div>
      </div>
      <div class="minor" v-if="fulfillmentType === 'PICKUP'">🏪 支付成功后请到店出示「动态核销码」提货，到店报手机尾号核销</div>
    </template>

    <!-- 吸底操作栏 -->
    <div class="bottom-bar">
      <template v-if="phase === 'form'">
        <div style="flex:1;font-size:14px">
          应付：<b style="color:var(--brand);font-size:18px">¥{{ yuan(goodsAmount + (fulfillmentType === 'PICKUP' || goodsAmount >= 9900 ? 0 : 600)) }}</b>
        </div>
        <button class="btn btn-buy" style="width:130px" :disabled="submitting" @click="submit">
          {{ submitting ? '提交中…' : '提交订单' }}
        </button>
      </template>
      <template v-else>
        <button class="btn btn-gray" style="flex:1" @click="$router.replace('/orders')">暂不支付</button>
        <button class="btn btn-buy" style="flex:1" :disabled="paying" @click="payNow">
          {{ paying ? '支付中…' : '立即支付 ¥' + yuan(orderInfo.payAmount) }}
        </button>
      </template>
    </div>

    <!-- 自提门店选择弹层 -->
    <div class="mask" v-if="showStoreSheet" @click="showStoreSheet = false">
      <div class="sheet" style="padding-bottom:24px" @click.stop>
        <span class="sheet-close" @click="showStoreSheet = false">✕</span>
        <div style="font-family:var(--kai);font-size:15px;letter-spacing:1px;margin-bottom:6px">选择自提门店</div>
        <div v-for="s in stores" :key="s.id" class="store-card" :style="{ opacity: s.status !== 'OPEN' ? .5 : 1 }" @click="storeId = s.id; showStoreSheet = false">
          <div class="s-ico">店</div>
          <div class="s-t"><b>{{ s.name }}</b>{{ s.address }}<br>☎ {{ s.phone }}</div>
          <span :style="{ color: storeId === s.id ? 'var(--brand)' : '#cfc4b0' }">{{ storeId === s.id ? '✓' : '›' }}</span>
        </div>
      </div>
    </div>
  </div>

  <div v-else-if="loaded">
    <div class="result" style="padding-top:110px">
      <div style="font-size:48px;margin-bottom:12px">🛒</div>
      <p>没有待结算的商品</p>
      <router-link to="/cart" class="btn btn-main result-btn" style="display:inline-flex">去购物车</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api, yuan, uuid } from '../api'
import { store } from '../store'
import { showToast, showAlert } from '../ui'
import GoodsMedia from '../components/GoodsMedia.vue'

const router = useRouter()
const items = ref([])
const stores = ref([])
const address = ref(null)
const member = ref(null)
const fulfillmentType = ref('EXPRESS')
const storeId = ref(null)
const usePoints = ref(false)
const showStoreSheet = ref(false)
const phase = ref('form')
const orderInfo = ref(null)
const submitting = ref(false)
const paying = ref(false)
const loaded = ref(false)
let idempotentKey = uuid()

const goodsAmount = computed(() => items.value.reduce((a, i) => a + i.price * i.quantity, 0))
const pickupStore = computed(() => stores.value.find(s => s.id === storeId.value) || null)

function maskPhone(p) { return p ? p.replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2') : '' }
function goBack() {
  if (window.history.length > 1) router.back()
  else router.replace('/cart')
}
function pickAddress() {
  router.push('/addresses?from=checkout')
}
async function submit() {
  if (fulfillmentType.value === 'EXPRESS' && !address.value) { showToast('请选择收货地址'); return }
  if (fulfillmentType.value === 'PICKUP' && !storeId.value) { showToast('请选择自提门店'); return }
  submitting.value = true
  try {
    const payload = {
      idempotentKey,
      fulfillmentType: fulfillmentType.value,
      usePoints: usePoints.value,
      items: items.value.map(i => ({ skuId: i.skuId, quantity: i.quantity }))
    }
    if (fulfillmentType.value === 'EXPRESS') payload.addressId = address.value.id
    else payload.storeId = storeId.value
    // 页内切换到「订单已提交」金额明细态
    orderInfo.value = await api.createOrder(payload)
    idempotentKey = uuid()
    phase.value = 'amount'
  } catch (e) {
    // 库存不足等业务失败：弹窗展示缺货 SKU 与剩余可购数量
    showAlert('下单失败', e.message)
  } finally { submitting.value = false }
}
async function payNow() {
  paying.value = true
  try {
    await api.pay(orderInfo.value.orderNo)
    router.replace(`/pay-result/${orderInfo.value.orderNo}?from=checkout`)
  } finally { paying.value = false }
}

onMounted(async () => {
  try {
    if (store.buyNow) {
      items.value = [store.buyNow]
      store.buyNow = null // 用后即清，防止刷新复用旧数据
    } else {
      const cart = await api.cart()
      items.value = (cart.rows || []).filter(r => r.checked && !r.invalid)
    }
    const [storeList, addrList, me] = await Promise.all([
      api.stores(),
      api.addresses().catch(() => []),
      api.me()
    ])
    stores.value = storeList
    member.value = me
    address.value = addrList.find(a => a.id === store.checkoutAddressId) ||
      addrList.find(a => a.isDefault) || addrList[0] || null
    storeId.value = store.selectedStoreId || (storeList[0] && storeList[0].id)
  } finally { loaded.value = true }
})
</script>

<style scoped>
.o-item { display: flex; align-items: center; gap: 10px; padding: 8px 0; }
</style>
