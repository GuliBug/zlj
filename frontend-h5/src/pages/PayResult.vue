<template>
  <div v-if="order">
    <div class="navbar-plain"><div class="title">支付结果</div></div>
    <div class="result">
      <div class="ok">✓</div>
      <h3>下单成功</h3>
      <p v-if="isPickup">
        实付 ¥{{ yuan(order.payAmount) }}<br>
        到店自提：{{ pickupStoreName || '所选门店' }}<br>
        请到店报手机尾号核销取货
      </p>
      <p v-else>
        实付 ¥{{ yuan(order.payAmount) }}<br>
        掌柜承诺当日 15:00 前下单当日发出
      </p>
      <button class="btn btn-main result-btn" @click="$router.replace('/order/' + order.orderNo)">查看订单</button>
      <button class="btn btn-gray result-btn" @click="$router.replace('/')">返回首页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { api, yuan } from '../api'

const route = useRoute()
const order = ref(null)

const isPickup = computed(() => order.value && order.value.fulfillmentType === 'PICKUP')
const pickupStoreName = computed(() => {
  const o = order.value
  if (!o) return ''
  return o.storeName || (o.store && o.store.name) || ''
})

onMounted(async () => {
  const no = route.params.no
  // 从结算页带 pay=1 进入时补一击支付（幂等，已支付则忽略）
  if (route.query.pay === '1') {
    try { await api.pay(no) } catch { /* 幂等拒绝视为已支付 */ }
  }
  order.value = await api.orderDetail(no)
})
</script>
