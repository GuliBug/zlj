<template>
  <div class="has-tabbar">
    <!-- 品牌导航：印章 + 标题 + 副题 + 门店切换 + 底条 -->
    <BrandNav />

    <!-- 健康警示条 -->
    <div class="warnstrip">⚠️ 健康提示：长期过量咀嚼槟榔，有害口腔健康 ｜ 本平台不向未成年人销售</div>

    <!-- hero -->
    <div class="hero">
      <h2>掌柜恭迎 · 正隆記</h2>
      <p>正宗湘潭门店究头，门店同款 · 原厂直发<br>线下门店核销 / 线上下单均可用</p>
    </div>

    <!-- 四正宫格 -->
    <div class="four-grid">
      <div class="four"><b>正品</b><span>原厂直发</span></div>
      <div class="four"><b>正源</b><span>海南原果</span></div>
      <div class="four"><b>正工</b><span>古法工艺</span></div>
      <div class="four"><b>正心</b><span>四等精品</span></div>
    </div>

    <!-- 分档 chips（分类接口动态渲染，点击过滤） -->
    <div class="chips">
      <div class="chip" :class="{ on: filterCatId === null }" @click="filterCat(null)">全部</div>
      <div
v-for="c in categories" :key="c.id" class="chip" :class="{ on: filterCatId === c.id }"
        @click="filterCat(c.id)">{{ c.name }}</div>
    </div>

    <!-- 门店同款 -->
    <div class="sect-head">
      <h3>门店同款</h3><span class="sub">当日到店 · 同一批次</span>
      <span class="r" @click="$router.push('/category')">全部 ›</span>
    </div>
    <div class="grid">
      <div v-for="p in products" :key="p.id" class="goods" @click="$router.push('/product/' + p.id)">
        <GoodsMedia :image="p.image" :name="p.name" height="110px" char-size="34px">
          <span class="corner">{{ cornerOf(p) }}</span>
        </GoodsMedia>
        <div class="info">
          <div class="name">{{ p.name }}</div>
          <div class="desc">门店同款 · 原厂直发</div>
          <div class="row">
            <span class="price"><small>¥</small>{{ yuan(p.priceMin) }}<template v-if="p.priceMax > p.priceMin"><small> 起</small></template></span>
            <span class="sales">已售 {{ salesText(p.sales) }}</span>
          </div>
        </div>
      </div>
    </div>
    <div class="empty" v-if="loaded && !products.length">
      <div class="icon">🍃</div>暂无商品
    </div>
    <div class="muted load-more" v-if="hasMore" @click="loadProducts(false)">加载更多</div>

    <div style="height:20px"></div>
    <div class="minor">本商城为正品渠道示意 · 不向未成年人销售槟榔制品</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api, yuan } from '../api'
import BrandNav from '../components/BrandNav.vue'
import GoodsMedia from '../components/GoodsMedia.vue'

const categories = ref([])
const products = ref([])
const filterCatId = ref(null)
const page = ref(1)
const hasMore = ref(false)
const loaded = ref(false)

const cornerOf = (_p) => {
  const c = categories.value.find(x => x.id === filterCatId.value)
  return c ? c.name : '门店同款'
}
function salesText(n) {
  n = Number(n || 0)
  if (n >= 10000) return (n / 10000).toFixed(1).replace(/\.0$/, '') + '万'
  return String(n)
}
function filterCat(id) { filterCatId.value = id; loadProducts(true) }

async function loadCategories() {
  categories.value = (await api.categories()) || []
}
async function loadProducts(reset) {
  if (reset) { page.value = 1; products.value = [] }
  const d = await api.products({ categoryId: filterCatId.value || '', page: page.value, size: 10 })
  products.value = reset ? d.rows : products.value.concat(d.rows)
  hasMore.value = products.value.length < d.total
  page.value++
  loaded.value = true
}

onMounted(() => { loadCategories(); loadProducts(true) })
</script>

<style scoped>
.load-more { text-align: center; padding: 12px 0; }
</style>
