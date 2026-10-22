<template>
  <div class="has-tabbar" v-if="me">
    <!-- 品牌导航（紧凑版） -->
    <BrandNav compact />

    <!-- 掌柜卡 -->
    <div class="me-head">
      <div class="lv">掌柜卡 <small>{{ me.levelName }}</small></div>
      <div class="exp">📞 {{ me.phone }} ｜ 累计消费 ¥{{ yuan(me.totalPaid) }} ｜ 消费即攒积分，可在商城抵扣</div>
    </div>

    <!-- 四格统计 -->
    <div class="me-stats">
      <div class="me-stat" @click="showPoints = true">
        <b>{{ me.points }}</b><span>积分</span>
      </div>
      <div class="me-stat" @click="$router.push('/addresses')">
        <b>{{ addrCount }}</b><span>收货地址</span>
      </div>
      <div class="me-stat" @click="$router.push('/verify-code')">
        <b style="font-size:17px">⟩</b><span>门店核销码</span>
      </div>
      <div class="me-stat" @click="$router.push('/orders')">
        <b>{{ orderCount }}</b><span>订单</span>
      </div>
    </div>

    <!-- 菜单列表 -->
    <div class="menu">
      <div class="menu-item" @click="$router.push('/orders')">📦 我的订单 <span class="arr">›</span></div>
      <div class="menu-item" @click="$router.push('/aftersales')">↩️ 售后记录 <span class="arr">›</span></div>
      <div class="menu-item" @click="$router.push('/stores')">🏪 门店信息 <span class="arr">›</span></div>
      <div class="menu-item" @click="$router.push('/addresses')">📍 收货地址 <span class="arr">›</span></div>
      <div class="menu-item" @click="showService = true">☎️ 联系掌柜小二 <span class="arr">›</span></div>
    </div>

    <div class="minor">本平台不向未成年人销售槟榔制品</div>

    <div style="padding: 4px 14px 16px">
      <button class="btn btn-gray" style="width:100%" @click="logout">退出登录</button>
    </div>

    <!-- 积分明细弹层 -->
    <div class="mask" v-if="showPoints" @click="showPoints = false">
      <div class="sheet" style="padding-bottom:24px" @click.stop>
        <span class="sheet-close" @click="showPoints = false">✕</span>
        <div style="font-family:var(--kai);font-size:15px;letter-spacing:1px;margin-bottom:8px">积分明细（消费 1 元 = 1 积分）</div>
        <div v-if="logs.length">
          <div v-for="l in logs" :key="l.id" class="settle-row">
            <span style="min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">
              {{ l.reason }}<br><span style="font-size:10px;color:var(--text-2)">{{ l.createdAt }}</span>
            </span>
            <span :style="{ color: l.change >= 0 ? 'var(--brand)' : 'var(--text-2)' }">{{ l.change >= 0 ? '+' : '' }}{{ l.change }}</span>
          </div>
        </div>
        <div class="muted" v-else style="padding:12px 0">暂无积分记录</div>
      </div>
    </div>

    <!-- 客服弹层 -->
    <div class="mask" v-if="showService" @click="showService = false">
      <div class="sheet" style="border-top-color:var(--gold);text-align:center;padding-bottom:24px" @click.stop>
        <span class="sheet-close" @click="showService = false">✕</span>
        <div style="font-family:var(--kai);font-size:15px;letter-spacing:1px">联系掌柜小二</div>
        <div style="font-size:20px;font-weight:700;color:var(--brand);margin-top:12px">400-XXX-XXXX</div>
        <div class="muted" style="margin-top:6px;font-size:12px">工作日 9:00-18:00</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api, yuan, setToken } from '../api'
import { showToast } from '../ui'
import BrandNav from '../components/BrandNav.vue'

const router = useRouter()
const me = ref(null)
const addrCount = ref(0)
const orderCount = ref(0)
const logs = ref([])
const showPoints = ref(false)
const showService = ref(false)

function logout() {
  setToken('')
  showToast('已退出登录')
  router.replace('/login')
}

onMounted(async () => {
  me.value = await api.me()
  // 四格统计：地址数 / 订单数 / 积分明细
  api.addresses().then(l => { addrCount.value = (l || []).length }).catch(() => {})
  api.orders({ page: 1, size: 1 }).then(d => { orderCount.value = d.total || 0 }).catch(() => {})
  api.pointsLogs(1, 5).then(d => { logs.value = d.rows || [] }).catch(() => {})
})
</script>
