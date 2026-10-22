<template>
  <div class="has-tabbar has-bottom-bar" v-if="rows.length">
    <!-- 失效商品 -->
    <template v-if="invalidRows.length">
      <div class="sect-head"><h3>失效商品</h3><span class="sub">{{ invalidRows.length }} 件</span>
        <span class="r" @click="clearInvalid">一键清除</span></div>
      <div v-for="r in invalidRows" :key="r.id" class="cart-item invalid">
        <span class="tag grey">失效</span>
        <GoodsMedia :image="r.image" :name="r.name" height="56px" char-size="22px" style="width:56px;border-radius:8px;flex:none" />
        <div class="cart-mid">
          <div class="cart-name">{{ r.name }}</div>
          <span class="cart-sku">{{ r.flavor }} / {{ r.spec }}</span>
        </div>
      </div>
    </template>

    <!-- 购物车行 -->
    <div v-for="r in validRows" :key="r.id" class="cart-item">
      <div class="checkbox" :class="{ on: r.checked }" @click="toggle(r)">{{ r.checked ? '✓' : '' }}</div>
      <GoodsMedia :image="r.image" :name="r.name" height="56px" char-size="22px" style="width:56px;border-radius:8px;flex:none" />
      <div class="cart-mid">
        <div class="cart-name">{{ r.name }}</div>
        <span class="cart-sku">{{ r.flavor }} / {{ r.spec }}</span>
        <div v-if="r.quantity > r.stock" style="font-size:11px;color:var(--brand)">库存不足，仅剩 {{ r.stock }} 件</div>
        <div class="cart-bot">
          <span class="price" style="font-size:15px"><small>¥</small>{{ yuan(r.price) }}</span>
          <div class="stepper">
            <button @click="changeQty(r, r.quantity - 1)">−</button>
            <span>{{ r.quantity }}</span>
            <button :disabled="r.quantity >= r.stock" @click="changeQty(r, r.quantity + 1)">＋</button>
          </div>
        </div>
      </div>
      <div class="cart-del" @click="removeRow(r)">✕</div>
    </div>

    <!-- 吸底合计栏（类名 .bottom-bar / .btn 为 UI 自动化依赖，勿改） -->
    <div class="bottom-bar">
      <div style="flex:1;text-align:right">
        <span style="font-size:13px">合计：<b class="price" style="font-size:18px">¥{{ yuan(totalAmount) }}</b></span>
      </div>
      <button class="btn" style="width:120px" :disabled="!checkedCount" @click="goCheckout">去结算</button>
    </div>
  </div>

  <!-- 空态 -->
  <div class="has-tabbar" v-else-if="loaded">
    <div class="navbar-plain"><div class="title">购物车</div></div>
    <div class="result" style="padding-top:110px">
      <div style="font-size:48px;margin-bottom:12px">🛒</div>
      <p>购物车还是空的<br>掌柜恭迎您去逛逛</p>
      <router-link to="/" class="btn btn-main result-btn" style="display:inline-flex">去首页</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api, yuan } from '../api'
import { refreshCartCount } from '../store'
import { showToast, showConfirm } from '../ui'
import GoodsMedia from '../components/GoodsMedia.vue'

const router = useRouter()
const rows = ref([])
const loaded = ref(false)

const validRows = computed(() => rows.value.filter(r => !r.invalid))
const invalidRows = computed(() => rows.value.filter(r => r.invalid))
const checkedRows = computed(() => validRows.value.filter(r => r.checked))
const checkedCount = computed(() => checkedRows.value.reduce((a, r) => a + r.quantity, 0))
const totalAmount = computed(() => checkedRows.value.reduce((a, r) => a + r.price * r.quantity, 0))

async function load() {
  const d = await api.cart()
  rows.value = d.rows || []
  loaded.value = true
  refreshCartCount()
}
async function toggle(r) {
  await api.updateCart(r.id, { checked: !r.checked })
  r.checked = !r.checked
}
async function changeQty(r, q) {
  if (q < 1) { removeRow(r); return }
  await api.updateCart(r.id, { quantity: q })
  r.quantity = q
  refreshCartCount()
}
async function removeRow(r) {
  const ok = await showConfirm('删除商品', `确定将「${r.name}」移出购物车？`)
  if (!ok) return
  await api.deleteCart([r.id])
  rows.value = rows.value.filter(x => x.id !== r.id)
  refreshCartCount()
}
async function clearInvalid() {
  await api.clearInvalid()
  rows.value = rows.value.filter(r => !r.invalid)
  showToast('已清除失效商品')
}
function goCheckout() {
  if (!checkedCount.value) { showToast('请先勾选要结算的商品'); return }
  router.push('/checkout')
}

onMounted(load)
</script>
