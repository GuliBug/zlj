<template>
  <div>
    <div class="navbar-plain"><button class="back" @click="goBack">‹</button><div class="title">门店核销码</div></div>

    <!-- 无 orderId：展示当前门店信息 + 提示从订单进入 -->
    <template v-if="!orderId">
      <div class="hq-card">
        <div style="font-family:var(--kai);font-size:16px;letter-spacing:2px">到店出示 · 掌柜扫码核销</div>
        <div style="font-size:11px;color:var(--text-2);margin-top:4px">
          当前门店：<b>{{ currentStoreName || '未选择门店' }}</b>（可在门店信息中切换）
        </div>
        <div class="qr" style="opacity:.35"></div>
        <div class="barcode" style="opacity:.35"></div>
        <div class="code">— — — —</div>
        <div style="font-size:11px;color:var(--warn);margin-top:12px;line-height:1.7">
          核销码与订单绑定：请从「我的订单」中找到<b>待自提</b>订单，点击「出示核销码」或进入订单详情查看
        </div>
      </div>
      <div class="minor" style="margin-bottom:20px">
        <span style="color:var(--brand);cursor:pointer" @click="$router.push('/orders?tab=PICKUP_WAIT')">前往我的订单 ›</span>
      </div>
    </template>

    <!-- 有 orderId：动态核销码 -->
    <template v-else>
      <div class="hq-card">
        <div style="font-family:var(--kai);font-size:16px;letter-spacing:2px">到店出示 · 掌柜扫码核销</div>
        <div style="font-size:11px;color:var(--text-2);margin-top:4px">
          订单号：<b>{{ orderId }}</b>
        </div>
        <div class="qr"></div>
        <div class="barcode"></div>
        <div class="code" style="font-size:22px;color:var(--text);letter-spacing:8px;font-weight:700">
          {{ codeText || '· · · · · ·' }}
        </div>
        <div style="font-size:10px;color:var(--text-2);margin-top:10px;line-height:1.7">
          · 核销码每分钟自动刷新，防截图盗用<br>· 仅限门店现场核销使用，到店报手机尾号亦可
        </div>
      </div>
      <div class="minor">核销记录可在「我的-订单」中查看</div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { selectedStoreName } from '../store'
import { showAlert } from '../ui'

const route = useRoute()
const router = useRouter()
const orderId = String(route.query.orderId || '')
const rawCode = ref('')
const countdown = ref(0)
let timer = null

const codeText = computed(() => {
  const c = rawCode.value
  return c ? c.replace(/(\d{2})(?=\d)/g, '$1 ') : ''
})
const currentStoreName = computed(() => selectedStoreName())

function goBack() {
  if (window.history.length > 1) router.back()
  else router.replace('/member')
}
async function refreshCode() {
  try {
    const r = await api.verifyCode(orderId)
    rawCode.value = r.code
    countdown.value = Math.min(r.expiresInSec || 60, 60)
    clearInterval(timer)
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) refreshCode()
    }, 1000)
  } catch (e) {
    showAlert('核销码获取失败', e.message)
  }
}

onMounted(() => { if (orderId) refreshCode() })
onUnmounted(() => clearInterval(timer))
</script>
