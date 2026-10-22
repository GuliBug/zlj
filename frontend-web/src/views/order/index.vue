<template>
  <div class="panel-card">
    <h3 class="panel-title">
      订单管理
      <span class="r">
        <el-button size="small" plain @click="batchShipVisible = true">批量发货</el-button>
        <el-button size="small" plain @click="exportVisible = true">导出</el-button>
      </span>
    </h3>

    <!-- 筛选 -->
    <div class="filter-bar">
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
        <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
      <el-select v-model="query.storeId" placeholder="全部门店" clearable style="width: 150px">
        <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <el-input v-model="query.orderNo" placeholder="订单号" clearable style="width: 170px" @keyup.enter="load(true)" />
      <el-input v-model="query.phone" placeholder="买家手机号（脱敏查询）" clearable style="width: 180px" @keyup.enter="load(true)" />
      <el-button @click="load(true)">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="filteredRows" :row-class-name="overdueRowClass">
      <el-table-column label="订单号" min-width="180">
        <template #default="{ row }">
          {{ row.orderNo }}
          <div class="sub">微信支付 · {{ row.fulfillmentType === 'PICKUP' ? '到店自提' : '快递' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="商品" min-width="220">
        <template #default="{ row }">
          <div v-for="(it, i) in row.items || []" :key="i" class="goods-line">
            <span class="gthumb">🀄</span>
            <div>
              {{ it.name }}
              <div class="sub">{{ [it.flavor, it.spec].filter(Boolean).join(' ') }} ×{{ it.quantity }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="实付" width="100">
        <template #default="{ row }"><b>{{ fen2yuan(row.payAmount) }}</b></template>
      </el-table-column>
      <el-table-column label="买家" width="140">
        <template #default="{ row }">
          <span class="sub">{{ row.phone || '—' }}</span>
          <div v-if="row.region" class="sub">{{ row.region }}</div>
        </template>
      </el-table-column>
      <el-table-column label="下单时间" width="160">
        <template #default="{ row }">
          {{ row.createdAt }}
          <div v-if="row.shipOverdue" class="overdue-text">待发货超 24h ⚠</div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small" round effect="light">{{ row.statusText || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PAID'" type="primary" size="small" plain @click="openShip(row)">发货</el-button>
          <el-button v-if="row.status === 'PICKUP_WAIT'" type="primary" size="small" plain @click="$router.push('/stores')">去核销</el-button>
          <el-button v-if="row.status === 'COMPLETED' || row.status === 'SHIPPED'" link size="small" @click="showDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager">
      <el-pagination
v-model:current-page="query.page" v-model:page-size="query.size"
        layout="total, prev, pager, next" :total="total" @current-change="load" />
    </div>

    <!-- 发货弹窗 -->
    <el-dialog v-model="shipVisible" title="订单发货（仅待发货可发）" width="440px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="订单号"><span>{{ shipForm.orderNo }}</span></el-form-item>
        <el-form-item label="物流公司" required><el-input v-model="shipForm.company" placeholder="如：顺丰速运" /></el-form-item>
        <el-form-item label="物流单号" required><el-input v-model="shipForm.no" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="primary" @click="submitShip">确认发货</el-button>
      </template>
    </el-dialog>

    <!-- 批量发货弹窗 -->
    <el-dialog v-model="batchShipVisible" title="批量发货（Excel 导入物流单号，失败行回显原因）" width="640px" destroy-on-close>
      <el-input v-model="batchText" type="textarea" :rows="8" placeholder="订单号,物流公司,物流单号&#10;ZL2026102201,顺丰速运,SF1234567890" />
      <el-alert
v-if="batchFailures.length" type="error" :closable="false" style="margin-top: 10px"
        :title="`成功 ${batchResult.success || 0} 单，失败 ${batchFailures.length} 单`" />
      <el-table v-if="batchFailures.length" :data="batchFailures" size="small" style="margin-top: 8px">
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column prop="reason" label="失败原因" min-width="200" />
      </el-table>
      <template #footer>
        <el-button @click="batchShipVisible = false">关闭</el-button>
        <el-button type="primary" @click="submitBatchShip">解析并提交</el-button>
      </template>
    </el-dialog>

    <!-- 导出弹窗 -->
    <el-dialog v-model="exportVisible" title="导出订单 CSV" width="420px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="时间范围">
          <el-date-picker v-model="exportRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" style="width: 260px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exportVisible = false">取消</el-button>
        <el-button type="primary" @click="doExport">导出</el-button>
      </template>
    </el-dialog>

    <!-- 订单详情弹窗（契约无管理端订单详情接口，展示列表已有字段） -->
    <el-dialog v-model="detailVisible" title="订单详情" width="480px" destroy-on-close>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="订单号">{{ detailRow.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detailRow.statusText || detailRow.status }}</el-descriptions-item>
        <el-descriptions-item label="实付金额">{{ fen2yuan(detailRow.payAmount) }}</el-descriptions-item>
        <el-descriptions-item label="买家">{{ detailRow.phone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="履约方式">{{ detailRow.fulfillmentType === 'PICKUP' ? '到店自提' : '快递' }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ detailRow.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="商品">
          <div v-for="(it, i) in detailRow.items || []" :key="i">
            {{ it.name }}（{{ [it.flavor, it.spec].filter(Boolean).join(' ') }}）× {{ it.quantity }}
          </div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listOrders, shipOrder, batchShip, exportOrders } from '@/api/order'
import { listStores } from '@/api/store'
import { fen2yuan, downloadCsv } from '@/utils/format'

const statusOptions = [
  { value: 'WAIT_PAY', label: '待支付' },
  { value: 'PAID', label: '待发货' },
  { value: 'SHIPPED', label: '待收货' },
  { value: 'PICKUP_WAIT', label: '待自提' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CLOSED', label: '已关闭' },
  { value: 'AFTER_SALE', label: '售后中' }
]

const query = reactive({ page: 1, size: 10, status: null, storeId: null, orderNo: '', phone: '' })
const rows = ref([])
const total = ref(0)
const stores = ref([])

const shipVisible = ref(false)
const shipForm = reactive({ orderNo: '', company: '', no: '' })

const batchShipVisible = ref(false)
const batchText = ref('')
const batchFailures = ref([])
const batchResult = ref({})

const exportVisible = ref(false)
const exportRange = ref(null)

const detailVisible = ref(false)
const detailRow = ref({})

function statusTag(status) {
  return { WAIT_PAY: 'warning', PAID: 'warning', SHIPPED: '', PICKUP_WAIT: 'warning', COMPLETED: 'success', CLOSED: 'info', AFTER_SALE: 'danger' }[status] || 'info'
}

function overdueRowClass({ row }) {
  return row.shipOverdue ? 'overdue-row' : ''
}

// 门店为前端过滤（列表行含 storeName/storeId 时生效）
const filteredRows = computed(() => {
  if (!query.storeId) return rows.value
  const store = stores.value.find((s) => s.id === query.storeId)
  if (!store) return rows.value
  return rows.value.filter((r) => r.storeId === query.storeId || r.storeName === store.name)
})

async function load(reset) {
  if (reset) query.page = 1
  const params = { page: query.page, size: query.size }
  if (query.status) params.status = query.status
  // 契约 keyword 同时匹配订单号/手机号
  const kw = query.orderNo || query.phone
  if (kw) params.keyword = kw
  const res = await listOrders(params)
  rows.value = res.rows
  total.value = res.total
}

function resetQuery() {
  Object.assign(query, { page: 1, status: null, storeId: null, orderNo: '', phone: '' })
  load()
}

function openShip(row) {
  Object.assign(shipForm, { orderNo: row.orderNo, company: '', no: '' })
  shipVisible.value = true
}

async function submitShip() {
  if (!shipForm.company || !shipForm.no) return ElMessage.warning('物流公司与单号必填')
  await shipOrder(shipForm.orderNo, { company: shipForm.company, no: shipForm.no })
  ElMessage.success('已发货，买家端可见物流单号')
  shipVisible.value = false
  load()
}

async function submitBatchShip() {
  const parsed = batchText.value
    .split('\n')
    .map((l) => l.trim())
    .filter(Boolean)
    .map((l) => {
      const parts = l.split(/[,，]/).map((s) => s.trim())
      return { orderNo: parts[0], company: parts[1] || '', no: parts[2] || '' }
    })
    .filter((r) => r.orderNo)
  if (!parsed.length) return ElMessage.warning('请按「订单号,物流公司,单号」每行一条粘贴')
  batchFailures.value = []
  batchResult.value = {}
  const res = await batchShip(parsed)
  batchResult.value = res.data || {}
  batchFailures.value = (res.data && res.data.failures) || []
  ElMessage.success(`批量发货完成：成功 ${res.data?.success ?? 0} 单`)
  load()
}

async function doExport() {
  const params = {}
  if (exportRange.value && exportRange.value.length === 2) {
    params.from = exportRange.value[0]
    params.to = exportRange.value[1]
  }
  const blob = await exportOrders(params)
  downloadCsv(await blob.text(), `订单导出_${Date.now()}.csv`)
  ElMessage.success('异步导出任务已创建')
  exportVisible.value = false
}

function showDetail(row) {
  detailRow.value = row
  detailVisible.value = true
}

onMounted(async () => {
  load()
  try {
    const res = await listStores()
    stores.value = res.data || []
  } catch {
      /* 忽略：降级保持占位 */
    }
})
</script>

<style scoped>
.goods-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.gthumb {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: #f3ead9;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  flex: none;
}
.overdue-text {
  color: var(--orange);
  font-size: 11px;
}
</style>
