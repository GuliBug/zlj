<template>
  <div class="has-tabbar">
    <div class="navbar-plain"><div class="title">全部商品</div></div>
    <div class="warnstrip">⚠️ 健康提示：长期过量咀嚼槟榔，有害口腔健康 ｜ 本平台不向未成年人销售</div>

    <template v-if="sections.length">
      <template v-for="sec in sections" :key="sec.id">
        <div class="sect-head">
          <h3>{{ sec.name }}</h3><span class="sub">门店头道工艺 · 同款同源</span>
        </div>
        <div class="grid">
          <div v-for="p in sec.rows" :key="p.id" class="goods" @click="$router.push('/product/' + p.id)">
            <GoodsMedia :image="p.image" :name="p.name" height="110px" char-size="34px">
              <span class="corner">{{ sec.name }}</span>
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
        <div class="empty" v-if="!sec.rows.length" style="padding:20px 0">该分类暂无商品</div>
      </template>
    </template>
    <div class="empty" v-else-if="loaded"><div class="icon">🍃</div>暂无商品</div>
    <div style="height:20px"></div>
    <div class="minor">本平台不向未成年人销售槟榔制品</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api, yuan } from '../api'
import GoodsMedia from '../components/GoodsMedia.vue'

// 按顶级分类分节渲染（对齐原型「究头系列 / 礼盒典藏」分节网格）
const sections = ref([])
const loaded = ref(false)

function salesText(n) {
  n = Number(n || 0)
  if (n >= 10000) return (n / 10000).toFixed(1).replace(/\.0$/, '') + '万'
  return String(n)
}

onMounted(async () => {
  const tree = (await api.categories()) || []
  const tops = tree.length ? tree : [{ id: '', name: '全部商品' }]
  const results = await Promise.all(tops.map(async (c) => {
    try {
      const d = await api.products({ categoryId: c.id || '', page: 1, size: 50 })
      return { id: c.id || 'all', name: c.name, rows: d.rows || [] }
    } catch {
      return { id: c.id || 'all', name: c.name, rows: [] }
    }
  }))
  sections.value = results
  loaded.value = true
})
</script>
