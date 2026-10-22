<template>
  <div>
    <!-- 4 指标卡 -->
    <div class="cards">
      <div class="stat clickable" @click="$router.push('/orders')">
        <span class="tag org">待处理</span>
        <b>{{ dash.pendingShip ?? '—' }}</b>
        <span>待发货订单（超24h：<b class="orange">{{ overdueShip }}</b>）</span>
      </div>
      <div class="stat clickable" @click="$router.push('/aftersales')">
        <span class="tag red">待处理</span>
        <b>{{ dash.pendingAftersale ?? '—' }}</b>
        <span>待审核售后工单（超48h：<b class="red-t">{{ overdueAudit }}</b>）</span>
      </div>
      <div class="stat clickable" @click="$router.push('/stock')">
        <span class="tag org">预警</span>
        <b>{{ dash.stockAlertCount ?? '—' }}</b>
        <span>库存低于安全阈值 SKU</span>
      </div>
      <div class="stat">
        <span class="tag grn">今日</span>
        <b>{{ fen2yuan(dash.gmvToday) }}</b>
        <span>GMV · 支付成功率 {{ payRateText }} · 到店核销 {{ verifyToday }} 单</span>
      </div>
    </div>

    <!-- 今日概览 -->
    <div class="panel-card">
      <h3 class="panel-title">
        今日概览
        <span class="r"><el-button size="small" plain @click="exportTip">导出报表</el-button></span>
      </h3>
      <el-table :data="overviewRows" :border="false" size="default">
        <el-table-column prop="metric" label="指标" width="130" />
        <el-table-column prop="caliber" label="口径">
          <template #default="{ row }"><span class="sub">{{ row.caliber }}</span></template>
        </el-table-column>
        <el-table-column prop="today" label="今日" width="130" />
        <el-table-column prop="yesterday" label="昨日" width="130" />
        <el-table-column prop="delta" label="环比" width="130">
          <template #default="{ row }">
            <span :class="{ 'green-t': row.deltaColor === 'green', 'red-t': row.deltaColor === 'red' }">{{ row.delta }}</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="timeline-note">
        资损监控：重复支付 / 金额异常 / 状态回跳 / 对账差异超 24h 未处理 → 告警推送财务+超管（30 分钟未响应升级短信）。
        今日告警：<b :class="alertCount > 0 ? 'red-t' : ''">{{ alertCount || 0 }}</b> 条。
        <el-button link type="primary" size="small" @click="$router.push('/audit-alerts')">进入审计与告警 →</el-button>
      </div>
    </div>

    <!-- 近 7 日销售趋势 -->
    <div class="panel-card">
      <h3 class="panel-title">近 7 日销售趋势</h3>
      <div class="trend-box" v-if="trend.length">
        <div class="trend-col" v-for="d in trend" :key="d.date">
          <div class="trend-gmv">{{ fen2yuan(d.gmv) }}</div>
          <div class="trend-bar-wrap">
            <div class="trend-bar" :style="{ height: barHeight(d.gmv) }"></div>
          </div>
          <div class="trend-order">{{ d.orderCount }} 单</div>
          <div class="trend-date">{{ d.date.slice(5) }}</div>
        </div>
      </div>
      <el-empty v-else description="暂无数据" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getDashboard } from '@/api/dashboard'
import { listOrders } from '@/api/order'
import { listAftersales } from '@/api/aftersale'
import { listVerifyRecords } from '@/api/store'
import { listAlerts } from '@/api/audit'
import { fen2yuan } from '@/utils/format'

const dash = ref({})
const overdueShip = ref('—')
const overdueAudit = ref('—')
const verifyToday = ref('—')
const alertCount = ref(0)

const trend = computed(() => dash.value.salesTrend || [])
const maxGmv = computed(() => Math.max(...trend.value.map((d) => d.gmv || 0), 1))
const payRateText = computed(() => (dash.value.payRate != null ? dash.value.payRate + '%' : '—'))

// 昨日数据从 salesTrend 推导（接口无独立昨日字段）
const yesterdayRow = computed(() => {
  const d = new Date(Date.now() - 86400000)
  const key = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
  return trend.value.find((t) => t.date === key)
})

function pctDelta(today, yesterday, unit) {
  if (today == null || yesterday == null || yesterday === 0) return '—'
  const v = ((today - yesterday) / yesterday) * 100
  const s = (v >= 0 ? '+' : '') + v.toFixed(1) + (unit || '%')
  return { text: s, up: v >= 0 }
}

const overviewRows = computed(() => {
  const gmvToday = dash.value.gmvToday
  const gmvYd = yesterdayRow.value ? yesterdayRow.value.gmv : null
  const gmvDelta = pctDelta(gmvToday, gmvYd)
  return [
    {
      metric: 'GMV',
      caliber: '支付成功订单实付金额（不含全额退款）',
      today: fen2yuan(gmvToday),
      yesterday: gmvYd != null ? fen2yuan(gmvYd) : '—',
      delta: gmvDelta ? gmvDelta.text : '—',
      deltaColor: gmvDelta && gmvDelta.up ? 'green' : 'red'
    },
    {
      metric: '支付成功率',
      caliber: '支付成功订单 ÷ 提交订单',
      today: dash.value.payRate != null ? dash.value.payRate + '%' : '—',
      yesterday: '—',
      delta: '—',
      deltaColor: ''
    },
    {
      metric: '售后率',
      caliber: '售后申请单 ÷ 已完成订单（月）',
      today: '—',
      yesterday: '—',
      delta: '—',
      deltaColor: ''
    },
    {
      metric: '待处理对账差异',
      caliber: 'T+1 渠道账单 vs 平台流水',
      today: '—',
      yesterday: '—',
      delta: '—',
      deltaColor: ''
    }
  ]
})

function barHeight(gmv) {
  return Math.max(4, Math.round((gmv / maxGmv.value) * 120)) + 'px'
}

function exportTip() {
  ElMessage.info('报表导出接口暂未在契约中提供（AC-09 dashboard 无导出端点）')
}

onMounted(async () => {
  const res = await getDashboard()
  dash.value = res.data || {}

  // 超24h未发货数：orders 接口 overdueShip 过滤后取 total
  try {
    const r = await listOrders({ overdueShip: true, page: 1, size: 1 })
    overdueShip.value = r.total
  } catch {
      /* 忽略：降级保持占位 */
    }

  // 超48h未审核售后数
  try {
    const r = await listAftersales({ overdue: true, page: 1, size: 1 })
    overdueAudit.value = r.total
  } catch {
      /* 忽略：降级保持占位 */
    }

  // 今日到店核销单数：核销记录接口无日期参数，按 verifyTime 前缀在首页数据内推导
  try {
    const d = new Date()
    const todayKey = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
    const r = await listVerifyRecords({ page: 1, size: 100 })
    verifyToday.value = (r.rows || []).filter((x) => (x.verifyTime || '').startsWith(todayKey)).length
  } catch {
      /* 忽略：降级保持占位 */
    }

  // 今日告警数
  try {
    const r = await listAlerts({ page: 1, size: 1 })
    alertCount.value = r.total
  } catch {
      /* 忽略：降级保持占位 */
    }
})
</script>

<style scoped>
.cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}
.stat {
  background: var(--card);
  border-radius: var(--radius);
  border: 1px solid var(--line);
  padding: 16px;
  transition: box-shadow 0.15s;
}
.stat.clickable {
  cursor: pointer;
}
.stat.clickable:hover {
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.06);
}
.stat b {
  font-size: 24px;
  display: block;
  margin: 6px 0 2px;
}
.stat > span {
  color: var(--text-2);
  font-size: 12px;
}
.tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
}
.tag.red {
  background: var(--primary-light);
  color: var(--primary);
}
.tag.org {
  background: #fff4e0;
  color: var(--orange);
}
.tag.grn {
  background: #e8f5e9;
  color: var(--green);
}
.orange {
  color: var(--orange);
}
.red-t {
  color: var(--primary);
}
.green-t {
  color: var(--green);
}
.trend-box {
  display: flex;
  align-items: flex-end;
  gap: 24px;
  padding: 10px 20px;
}
.trend-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.trend-gmv {
  font-size: 12px;
  color: var(--primary);
}
.trend-bar-wrap {
  height: 130px;
  display: flex;
  align-items: flex-end;
}
.trend-bar {
  width: 36px;
  background: linear-gradient(180deg, var(--gold), var(--primary));
  border-radius: 4px 4px 0 0;
}
.trend-order {
  font-size: 12px;
  color: var(--text-2);
}
.trend-date {
  font-size: 12px;
  color: var(--text-2);
}
</style>
