<template>
  <div class="panel-card">
    <h3 class="panel-title">
      库存管理
      <span class="r">
        <el-button type="primary" size="small" @click="openInboundFromSelect">＋ 入库</el-button>
        <el-button size="small" plain @click="openStocktake">发起盘点</el-button>
      </span>
    </h3>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- Tab 1 库存总览 -->
      <el-tab-pane label="库存总览" name="overview">
        <div class="filter-bar">
          <el-input v-model="skuQuery.keyword" placeholder="SKU / 条码 / 商品名" clearable style="width: 200px" @keyup.enter="loadSkus" />
          <el-select v-model="visualFilter" style="width: 120px" @change="() => {}">
            <el-option label="全部" value="all" />
            <el-option label="仅看预警" value="warn" />
            <el-option label="仅看冻结" value="frozen" />
          </el-select>
          <el-button type="primary" @click="loadSkus">查询</el-button>
        </div>
        <el-table :data="displaySkuRows">
          <el-table-column label="商品 / SKU" min-width="200">
            <template #default="{ row }">
              {{ row.productName }}
              <div class="sub">{{ row.id }} · {{ row.flavor || '' }} {{ row.spec || '' }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="physical" label="实物库存" width="90" />
          <el-table-column prop="reserved" label="预占" width="80" />
          <el-table-column label="可用库存" width="90">
            <template #default="{ row }">
              <b :style="{ color: row.available <= row.threshold ? 'var(--primary)' : 'inherit' }">{{ row.available }}</b>
            </template>
          </el-table-column>
          <el-table-column prop="frozen" label="冻结" width="80" />
          <el-table-column prop="threshold" label="安全阈值" width="90">
            <template #default="{ row }">
              <el-button link size="small" @click="openThreshold(row)">阈值 {{ row.threshold }}</el-button>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.status === 'FROZEN'" type="info" size="small" round effect="light">已冻结</el-tag>
              <el-tag v-else-if="row.available <= 10" type="danger" size="small" round effect="light">紧急预警</el-tag>
              <el-tag v-else-if="row.available <= row.threshold" type="warning" size="small" round effect="light">低于阈值</el-tag>
              <el-tag v-else type="success" size="small" round effect="light">充足</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" plain @click="openInbound(row)">入库</el-button>
              <el-button v-if="row.status !== 'FROZEN'" link type="warning" size="small" @click="openFreeze(row, 'FREEZE')">冻结</el-button>
              <el-button v-else link type="success" size="small" @click="openFreeze(row, 'RELEASE')">释放</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pager">
          <el-pagination
v-model:current-page="skuQuery.page" v-model:page-size="skuQuery.size"
            layout="total, prev, pager, next" :total="skuTotal" @current-change="loadSkus" />
        </div>
        <div class="timeline-note">
          库存口径：<b>实物库存 = 可用 + 预占 + 冻结</b>。扣减链路：下单预占（乐观锁）→ 支付成功实际扣减 → 取消/超时 15 分钟释放预占（兜底任务）。点击「阈值」可直接修改，修改留审计日志。
        </div>
      </el-tab-pane>

      <!-- Tab 2 库存流水 -->
      <el-tab-pane label="库存流水" name="flows">
        <div class="filter-bar">
          <el-input v-model="flowQuery.skuId" placeholder="SKU ID" clearable style="width: 140px" />
          <el-select v-model="flowQuery.type" placeholder="全部类型" clearable style="width: 150px">
            <el-option v-for="t in flowTypes" :key="t" :label="flowTypeText(t)" :value="t" />
          </el-select>
          <el-button type="primary" @click="loadFlows(true)">查询</el-button>
        </div>
        <el-table :data="flowRows">
          <el-table-column prop="createdAt" label="时间" width="170" />
          <el-table-column label="类型" width="110">
            <template #default="{ row }">
              <el-tag :type="['IN', 'REFUND_IN', 'UNFREEZE'].includes(row.type) ? 'success' : 'warning'" size="small" round effect="light">
                {{ flowTypeText(row.type) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="SKU" min-width="170">
            <template #default="{ row }">{{ row.productName }} <span class="sub">{{ row.skuId }}</span></template>
          </el-table-column>
          <el-table-column label="变动" width="80">
            <template #default="{ row }">
              <b :style="{ color: row.quantity >= 0 ? 'var(--green)' : 'var(--primary)' }">{{ row.quantity >= 0 ? '+' : '' }}{{ row.quantity }}</b>
            </template>
          </el-table-column>
          <el-table-column prop="balanceAfter" label="变动后可用" width="100" />
          <el-table-column prop="relatedNo" label="关联单号" min-width="160" show-overflow-tooltip />
          <el-table-column prop="remark" label="操作" min-width="120" show-overflow-tooltip />
        </el-table>
        <div class="pager">
          <el-pagination
v-model:current-page="flowQuery.page" v-model:page-size="flowQuery.size"
            layout="total, prev, pager, next" :total="flowTotal" @current-change="loadFlows" />
        </div>
        <div class="timeline-note">
          流水不可删除、不可修改；每笔变动必须关联业务单号（入库单 / 订单号 / 售后单 / 盘点单），日终以「实物库存 = Σ流水」核对，差异告警。
        </div>
      </el-tab-pane>

      <!-- Tab 3 盘点 -->
      <el-tab-pane label="盘点单" name="stocktake">
        <el-table :data="takeRows">
          <el-table-column label="盘点单号" width="110">
            <template #default="{ row }">PD{{ row.id }}</template>
          </el-table-column>
          <el-table-column label="SKU / 系统数 / 实盘数 / 差异" min-width="300">
            <template #default="{ row }">
              <div v-for="(it, i) in row.items || []" :key="i" class="take-item">
                {{ it.productName }}：系统 {{ it.systemCount }} / 实盘 {{ it.actual }} /
                <b :style="{ color: it.diff === 0 ? 'inherit' : it.diff > 0 ? 'var(--green)' : 'var(--primary)' }">差异 {{ it.diff > 0 ? '+' : '' }}{{ it.diff }}</b>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="提交人 / 时间" width="200">
            <template #default="{ row }">{{ row.createdBy || '—' }} / {{ row.createdAt }}</template>
          </el-table-column>
          <el-table-column label="状态" width="130">
            <template #default="{ row }">
              <el-tag :type="row.status === 'CONFIRMED' ? 'success' : 'warning'" size="small" round effect="light">
                {{ row.status === 'CONFIRMED' ? '已确认' : '待管理员确认' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status !== 'CONFIRMED'" type="primary" size="small" plain @click="confirmTake(row)">确认修正</el-button>
              <span v-else class="sub">—</span>
            </template>
          </el-table-column>
        </el-table>
        <div class="pager">
          <el-pagination
v-model:current-page="takeQuery.page" v-model:page-size="takeQuery.size"
            layout="total, prev, pager, next" :total="takeTotal" @current-change="loadStocktakes" />
        </div>
        <div class="timeline-note">
          盘点流程：库管员提交盘点单（填写实盘数，差异自动计算）→ <b>管理员二级确认</b> → 确认后按差异修正库存并生成「盘点修正」流水。提交人不得同时担任确认人。
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 入库弹窗 -->
    <el-dialog v-model="inboundVisible" title="商品入库" width="440px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="SKU" required>
          <el-select v-model="inbound.skuId" filterable style="width: 100%">
            <el-option v-for="s in allSkus" :key="s.id" :label="`${s.productName} ${s.flavor}/${s.spec}（现可用 ${s.available}）`" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量" required><el-input-number v-model="inbound.quantity" :min="1" style="width: 160px" /></el-form-item>
        <el-form-item label="批次号" required><el-input v-model="inbound.batchNo" placeholder="如 B20261022-01" /></el-form-item>
        <el-form-item label="供应商"><el-input v-model="inbound.supplier" placeholder="选填" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="inbound.remark" placeholder="选填" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inboundVisible = false">取消</el-button>
        <el-button type="primary" @click="submitInbound">确认</el-button>
      </template>
    </el-dialog>

    <!-- 冻结/释放弹窗 -->
    <el-dialog v-model="freezeVisible" :title="freezeAction === 'FREEZE' ? '冻结库存' : '释放冻结'" width="440px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="SKU"><span>{{ freezeRow.skuId }}（{{ freezeRow.skuName }}）</span></el-form-item>
        <el-form-item label="原因" required><el-input v-model="freezeReason" type="textarea" :rows="2" placeholder="必填" /></el-form-item>
      </el-form>
      <el-alert
v-if="freezeAction === 'FREEZE'" type="warning" :closable="false"
        title="冻结后可用库存清零，H5 端立即售罄置灰（预占订单不受影响）" />
      <template #footer>
        <el-button @click="freezeVisible = false">取消</el-button>
        <el-button type="warning" @click="submitFreeze">{{ freezeAction === 'FREEZE' ? '确认冻结' : '确认释放' }}</el-button>
      </template>
    </el-dialog>

    <!-- 阈值编辑弹窗 -->
    <el-dialog v-model="thresholdVisible" title="修改安全阈值（变更留审计）" width="420px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="SKU"><span>{{ thresholdRow.skuId }}（{{ thresholdRow.skuName }}）</span></el-form-item>
        <el-form-item label="阈值" required><el-input-number v-model="thresholdValue" :min="0" style="width: 160px" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="thresholdVisible = false">取消</el-button>
        <el-button type="primary" @click="submitThreshold">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新建盘点弹窗 -->
    <el-dialog v-model="takeVisible" title="发起盘点" width="720px" destroy-on-close>
      <div class="filter-bar">
        <el-select v-model="takeSkuId" filterable placeholder="选择 SKU（可搜索）" style="width: 340px">
          <el-option v-for="s in allSkus" :key="s.id" :label="`${s.id} ${s.productName} ${s.flavor}/${s.spec}（系统数 ${s.physical}）`" :value="s.id" />
        </el-select>
        <el-button type="primary" :disabled="takeSkuId == null" @click="addTakeItem">加入盘点</el-button>
      </div>
      <el-table :data="takeItems" size="small">
        <el-table-column prop="skuId" label="SKU ID" width="80" />
        <el-table-column prop="productName" label="商品" min-width="140" />
        <el-table-column prop="systemCount" label="系统数" width="90" />
        <el-table-column label="实盘数" width="120">
          <template #default="{ row }"><el-input-number v-model="row.actual" :min="0" :controls="false" size="small" style="width: 100px" /></template>
        </el-table-column>
        <el-table-column label="差异（自动计算）" width="130">
          <template #default="{ row }">
            <span :style="{ color: row.diff === 0 ? '#666' : row.diff > 0 ? 'var(--green)' : 'var(--primary)' }">{{ row.diff > 0 ? '+' : '' }}{{ row.diff }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ $index }"><el-button link type="danger" size="small" @click="takeItems.splice($index, 1)">移除</el-button></template>
        </el-table-column>
      </el-table>
      <el-form label-width="60px" style="margin-top: 12px">
        <el-form-item label="备注"><el-input v-model="takeRemark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="takeVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStocktake">提交盘点单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listSkus, stockInbound, stockFreeze, setThreshold, listFlows, createStocktake, listStocktakes, confirmStocktake } from '@/api/stock'

const flowTypes = ['IN', 'OUT', 'RESERVE', 'RELEASE', 'REFUND_IN', 'ADJUST', 'FREEZE', 'UNFREEZE']
const FLOW_TEXT = { IN: '入库', OUT: '订单扣减', RESERVE: '预占', RELEASE: '释放', REFUND_IN: '退款回补', ADJUST: '盘点修正', FREEZE: '冻结', UNFREEZE: '解冻' }
function flowTypeText(t) {
  return FLOW_TEXT[t] || t
}

const activeTab = ref('overview')
const visualFilter = ref('all')

// 总览
const skuQuery = reactive({ page: 1, size: 10, keyword: '' })
const skuRows = ref([])
const skuTotal = ref(0)

// 前端过滤：仅看预警 / 仅看冻结
const displaySkuRows = computed(() => {
  if (visualFilter.value === 'warn') return skuRows.value.filter((s) => s.available <= s.threshold)
  if (visualFilter.value === 'frozen') return skuRows.value.filter((s) => s.status === 'FROZEN')
  return skuRows.value
})

// 流水
const flowQuery = reactive({ page: 1, size: 10, skuId: '', type: null })
const flowRows = ref([])
const flowTotal = ref(0)

// 盘点
const takeQuery = reactive({ page: 1, size: 10 })
const takeRows = ref([])
const takeTotal = ref(0)
const takeVisible = ref(false)
const allSkus = ref([])
const takeSkuId = ref(null)
const takeItems = ref([])
const takeRemark = ref('')

function onTabChange(tab) {
  if (tab === 'overview') loadSkus()
  if (tab === 'flows') loadFlows()
  if (tab === 'stocktake') loadStocktakes()
}

async function loadSkus() {
  const res = await listSkus({ ...skuQuery })
  skuRows.value = res.rows
  skuTotal.value = res.total
}

async function loadFlows(reset) {
  if (reset) flowQuery.page = 1
  const params = { page: flowQuery.page, size: flowQuery.size }
  if (flowQuery.skuId) params.skuId = flowQuery.skuId
  if (flowQuery.type) params.type = flowQuery.type
  const res = await listFlows(params)
  flowRows.value = res.rows
  flowTotal.value = res.total
}

async function loadStocktakes() {
  const res = await listStocktakes({ ...takeQuery })
  takeRows.value = res.rows
  takeTotal.value = res.total
}

async function loadAllSkus() {
  const res = await listSkus({ page: 1, size: 500 })
  allSkus.value = res.rows || []
}

// 入库
const inboundVisible = ref(false)
const inbound = reactive({ skuId: null, quantity: 1, batchNo: '', supplier: '', remark: '' })

async function openInbound(row) {
  await loadAllSkus()
  inbound.skuId = row ? row.id : null
  Object.assign(inbound, { quantity: 1, batchNo: '', supplier: '', remark: '' })
  inboundVisible.value = true
}

async function openInboundFromSelect() {
  openInbound(null)
}

async function submitInbound() {
  if (!inbound.skuId) return ElMessage.warning('请选择 SKU')
  if (!inbound.quantity || inbound.quantity <= 0) return ElMessage.warning('数量必须大于 0')
  if (!inbound.batchNo.trim()) return ElMessage.warning('批次号必填')
  await stockInbound({ skuId: inbound.skuId, quantity: inbound.quantity, batchNo: inbound.batchNo, supplier: inbound.supplier, remark: inbound.remark })
  ElMessage.success('入库成功，流水已记录（关联入库单号）')
  inboundVisible.value = false
  loadSkus()
}

// 冻结/释放
const freezeVisible = ref(false)
const freezeRow = reactive({ skuId: null, skuName: '' })
const freezeAction = ref('FREEZE')
const freezeReason = ref('')

function openFreeze(row, action) {
  Object.assign(freezeRow, { skuId: row.id, skuName: `${row.productName} ${row.flavor}/${row.spec}` })
  freezeAction.value = action
  freezeReason.value = ''
  freezeVisible.value = true
}

async function submitFreeze() {
  if (!freezeReason.value.trim()) return ElMessage.warning('原因必填')
  const label = freezeAction.value === 'FREEZE' ? '冻结' : '释放'
  try {
    await ElMessageBox.confirm(`确定对 SKU ${freezeRow.skuId}（${freezeRow.skuName}）执行「${label}」操作吗？`, '二次确认', { type: 'warning' })
  } catch {
    return
  }
  await stockFreeze({ skuId: freezeRow.skuId, action: freezeAction.value, reason: freezeReason.value })
  ElMessage.success(label + '成功，流水已记录')
  freezeVisible.value = false
  loadSkus()
}

// 阈值
const thresholdVisible = ref(false)
const thresholdRow = reactive({ skuId: null, skuName: '' })
const thresholdValue = ref(0)

function openThreshold(row) {
  Object.assign(thresholdRow, { skuId: row.id, skuName: `${row.productName} ${row.flavor}/${row.spec}` })
  thresholdValue.value = row.threshold ?? 0
  thresholdVisible.value = true
}

async function submitThreshold() {
  await setThreshold({ skuId: thresholdRow.skuId, threshold: thresholdValue.value })
  ElMessage.success('阈值已更新，修改已写入审计日志')
  thresholdVisible.value = false
  loadSkus()
}

// 盘点
async function openStocktake() {
  takeSkuId.value = null
  takeItems.value = []
  takeRemark.value = ''
  await loadAllSkus()
  takeVisible.value = true
}

function addTakeItem() {
  const s = allSkus.value.find((x) => x.id === takeSkuId.value)
  if (!s) return
  if (takeItems.value.some((i) => i.skuId === s.id)) return ElMessage.warning('该 SKU 已在盘点列表中')
  takeItems.value.push({ skuId: s.id, productName: `${s.productName} ${s.flavor}/${s.spec}`, systemCount: s.physical, actual: s.physical, diff: 0 })
}

async function submitStocktake() {
  if (!takeItems.value.length) return ElMessage.warning('请加入至少一个 SKU')
  if (takeItems.value.some((i) => i.actual == null)) return ElMessage.warning('请填写所有实盘数')
  await createStocktake({
    items: takeItems.value.map((i) => ({ skuId: i.skuId, actual: i.actual })),
    remark: takeRemark.value
  })
  ElMessage.success('盘点单已提交，等待管理员二级确认')
  takeVisible.value = false
  activeTab.value = 'stocktake'
  loadStocktakes()
}

// 盘点二级确认（提交人≠确认人）
async function confirmTake(row) {
  try {
    await ElMessageBox.confirm(
      `确认盘点单 PD${row.id} 吗？确认后将按差异修正库存并生成「盘点修正」流水。\n注意：提交人（${row.createdBy || '-'}）不得同时担任确认人，若您是提交人，后端将拒绝。`,
      '盘点二级确认',
      { type: 'warning', customStyle: { whiteSpace: 'pre-line' } }
    )
    await ElMessageBox.confirm('再次确认：库存修正不可撤销，确定执行吗？', '最终确认', { type: 'error', confirmButtonText: '确定执行' })
  } catch {
    return
  }
  await confirmStocktake(row.id)
  ElMessage.success('盘点单已确认，库存已按差异修正')
  loadStocktakes()
}

onMounted(() => {
  loadSkus()
})
</script>

<style scoped>
.take-item {
  font-size: 12px;
  line-height: 1.8;
}
</style>
