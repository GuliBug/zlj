<template>
  <div class="has-bottom-bar" v-if="d">
    <div class="navbar-plain">
      <button class="back" @click="goBack">‹</button>
      <div class="title">商品详情</div>
      <div class="cart-ico" @click="$router.push('/cart')">🛒<span class="badge" v-if="store.cartCount > 0">{{ store.cartCount > 99 ? '99+' : store.cartCount }}</span></div>
    </div>

    <!-- 大图区（多图轮播；单图/破图兜底：渐变红底 + 楷体首字） -->
    <div class="d-img">
      <div class="d-swiper" v-if="imgs.length > 1" ref="swiperEl" @scroll.passive="onSwipe">
        <img v-for="(im, i) in imgs" :key="i" :src="im" alt="" />
      </div>
      <GoodsMedia v-else :image="mainImage" :name="d.name" height="250px" char-size="70px" />
      <span class="corner">{{ d.categoryName || '门店同款' }}</span>
      <div class="d-dots" v-if="imgs.length > 1">
        <i v-for="(im, i) in imgs" :key="i" :class="{ on: i === cur }"></i>
      </div>
    </div>

    <!-- 价格 / 已售 -->
    <div class="d-head">
      <span class="price" style="font-size:22px"><small>¥</small>{{ yuan(minPrice) }}</span>
      <span class="sales">已售 {{ totalSales }}</span>
    </div>
    <div class="d-name">{{ d.name }}</div>
    <div class="d-sub">{{ d.description || '门店同款 · 原厂直发 · 当日新鲜' }}</div>
    <div class="d-row">
      <span>📦 当日 15:00 前下单当日发</span>
      <span>🚚 运费 ¥6（满99包邮）</span>
    </div>

    <!-- 四正本纪 工艺文案块 -->
    <div class="craft">
      <b>四正本纪 · 门店究头</b>
      {{ d.description || DEFAULT_CRAFT }}
    </div>

    <!-- 详情图（管理端随商品保存，最多 6 张） -->
    <template v-if="detailImgs.length">
      <div class="detail-imgs-title">商品详情</div>
      <div class="d-detail-imgs">
        <img v-for="(im, i) in detailImgs" :key="i" :src="im" alt="" loading="lazy" />
      </div>
    </template>

    <!-- 健康警示 -->
    <div class="warnbox">⚠️ 健康警示：{{ d.warnText || '长期过量咀嚼槟榔，有害口腔健康' }}。请理性消费，适量食用。本页面仅作商品信息展示，不含任何功效宣传。</div>
    <div class="minor">本商品{{ d.minorText || '不向未成年人销售' }}</div>
    <div style="height:70px"></div>

    <!-- 吸底操作栏 -->
    <div class="bottom-bar" v-if="d.status === 'OFF'">
      <button class="btn btn-gray" style="flex:1" disabled>商品已下架</button>
    </div>
    <div class="bottom-bar" v-else>
      <button class="btn btn-cart" @click="openSku('cart')">加入购物车</button>
      <button class="btn btn-buy" @click="openSku('buy')">立即购买</button>
    </div>

    <!-- SKU 弹层 -->
    <div class="mask" v-if="showSku" @click.self="showSku = false">
      <div class="sheet">
        <span class="sheet-close" @click="showSku = false">✕</span>
        <div style="display:flex;gap:12px">
          <div class="cart-img" style="font-size:26px">
            <img v-if="mainImage && !imgBroken" :src="mainImage" alt="" style="width:100%;height:100%;object-fit:cover" @error="imgBroken = true" />
            <template v-else>{{ (d.name || '隆').charAt(0) }}</template>
          </div>
          <div>
            <div class="price">¥{{ pickedSku ? yuan(pickedSku.price) : yuan(minPrice) }}</div>
            <div style="font-size:11px;color:var(--text-2);margin-top:4px">
              库存 {{ pickedSku ? pickedSku.stock : totalStock }} 份
            </div>
          </div>
        </div>
        <div class="sku-label">规格 / 档位</div>
        <div class="sku-opts">
          <button
v-for="s in d.skus" :key="s.id" class="sku-opt"
            :class="{ on: pickedSkuId === s.id, disabled: s.stock <= 0 }"
            :disabled="s.stock <= 0" @click="pickSku(s)">
            {{ s.flavor }} / {{ s.spec }}<template v-if="s.stock <= 0">（缺货）</template>
          </button>
        </div>
        <div class="qty">
          <span style="font-size:13px">购买数量</span>
          <button @click="qty > 1 && qty--">−</button>
          <span>{{ qty }}</span>
          <button :disabled="pickedSku && qty >= pickedSku.stock" @click="qty++">＋</button>
        </div>
        <button
class="btn btn-main sku-ok" style="margin-top:18px" :disabled="!pickedSku || pickedSku.stock <= 0"
          @click="confirmSku">{{ skuMode === 'cart' ? '加入购物车' : '立即购买' }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, yuan } from '../api'
import { store, refreshCartCount } from '../store'
import { showToast, showAlert } from '../ui'
import GoodsMedia from '../components/GoodsMedia.vue'

const DEFAULT_CRAFT = '以海南原果为骨，守门店头道工艺：原果精挑 → 慢蒸杀青 → 晾片 → 闷香 → 老卤浸制。门店与线上同一批次，当日到店，每日新鲜。'

const route = useRoute()
const router = useRouter()
const d = ref(null)
const showSku = ref(false)
const pickedSkuId = ref(null)
const qty = ref(1)
const skuMode = ref('cart')
const imgBroken = ref(false)
const swiperEl = ref(null)
const cur = ref(0)

const mainImage = computed(() => ((d.value && d.value.images) || [])[0] || (d.value && d.value.image) || '')
const imgs = computed(() => (d.value && d.value.images) || [])
const detailImgs = computed(() => ((d.value && d.value.detailImages) || []).filter(Boolean))
function onSwipe() {
  const el = swiperEl.value
  if (el && el.clientWidth) cur.value = Math.round(el.scrollLeft / el.clientWidth)
}
const pickedSku = computed(() => (d.value.skus || []).find(s => s.id === pickedSkuId.value))
const minPrice = computed(() => {
  const skus = (d.value && d.value.skus) || []
  return skus.length ? Math.min(...skus.map(s => s.price)) : 0
})
const totalSales = computed(() => (d.value && d.value.skus || []).reduce((a, s) => a + (s.sales || 0), 0))
const totalStock = computed(() => (d.value && d.value.skus || []).reduce((a, s) => a + (s.stock || 0), 0))

function goBack() {
  if (window.history.length > 1) router.back()
  else router.replace('/')
}
function openSku(mode) {
  skuMode.value = mode
  qty.value = 1
  const skus = d.value.skus || []
  if (skus.length === 1 && skus[0].stock > 0) pickedSkuId.value = skus[0].id
  showSku.value = true
}
function pickSku(s) { pickedSkuId.value = s.id; qty.value = 1 }
async function confirmSku() {
  const sku = pickedSku.value
  if (!sku) { showToast('请选择规格'); return }
  if (sku.stock <= 0) { showToast('该规格已缺货'); return }
  if (skuMode.value === 'cart') {
    await api.addCart(sku.id, qty.value)
    await refreshCartCount()
    showSku.value = false
    showToast('已加入购物车')
  } else {
    showSku.value = false
    store.buyNow = {
      skuId: sku.id, quantity: qty.value, name: d.value.name,
      image: mainImage.value, price: sku.price,
      flavor: sku.flavor, spec: sku.spec
    }
    router.push('/checkout')
  }
}

onMounted(async () => {
  try {
    d.value = await api.product(route.params.id)
  } catch (e) {
    showAlert('商品加载失败', e.message)
  }
})
</script>
