<template>
  <div>
    <!-- 门店列表 -->
    <div class="panel-card">
      <h3 class="panel-title">
        门店列表
        <span class="r"><el-button type="primary" size="small" @click="openEdit(null)">＋ 新增门店</el-button></span>
      </h3>
      <div class="timeline-note" style="margin: 0 0 12px">H5 端买家可切换门店；商品、核销码均跟随所属门店。门店信息变更即时同步 H5。</div>
      <el-table :data="stores">
        <el-table-column label="门店" min-width="170">
          <template #default="{ row }">
            <b>{{ row.name }}</b>
            <el-tag v-if="row.isMain" size="small" type="danger" round effect="light" style="margin-left: 6px">主店</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="address" label="地址" min-width="230" show-overflow-tooltip />
        <el-table-column prop="phone" label="联系电话" width="150" />
        <el-table-column label="营业时间" width="130">
          <template #default="{ row }">{{ operatingHoursOf(row) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'OPEN' ? 'success' : 'warning'" size="small" round effect="light">
              {{ row.status === 'OPEN' ? '营业中' : '筹备中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 到店自提核销记录 -->
    <div class="panel-card">
      <h3 class="panel-title">到店自提核销记录</h3>

      <!-- 核销操作面板 -->
      <div class="verify-box">
        <span class="lab">核销操作</span>
        <el-input v-model="verifyForm.orderNo" placeholder="订单号" style="width: 220px" size="small" />
        <el-input v-model="verifyForm.code" placeholder="6 位核销码" maxlength="6" style="width: 130px" size="small" />
        <el-select v-model="verifyForm.storeId" placeholder="选择门店" style="width: 170px" size="small">
          <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-button type="primary" size="small" @click="doVerify">核销</el-button>
        <el-alert
v-if="verifyResult" :type="verifyResult.success ? 'success' : 'error'" :closable="false" style="margin-top: 8px"
          :title="verifyResult.success ? '核销成功：订单转「已完成」、库存实扣、留痕可追溯' : '核销失败：' + (verifyResult.msg || '未知原因')" />
      </div>

      <div class="filter-bar">
        <el-input v-model="recordKeyword" placeholder="核销码 / 手机尾号" clearable style="width: 180px" />
        <el-date-picker v-model="recordDate" type="date" value-format="YYYY-MM-DD" placeholder="日期" style="width: 150px" />
        <el-button @click="applyRecordFilter">查询</el-button>
        <span class="sub">核销记录接口不支持按码/日期查询时，为当前页前端过滤</span>
      </div>

      <el-table :data="displayVerifyRows">
        <el-table-column prop="orderNo" label="订单号" min-width="170" />
        <el-table-column prop="storeName" label="门店" width="140" />
        <el-table-column label="商品" min-width="180">
          <template #default="{ row }">
            <template v-if="row._goods">
              {{ row._goods.name }}
              <div class="sub">{{ row._goods.spec }} ×{{ row._goods.quantity }}</div>
            </template>
            <span v-else class="sub">—</span>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="100">
          <template #default="{ row }"><b>{{ fen2yuan(row.payAmount) }}</b></template>
        </el-table-column>
        <el-table-column label="核销时间" width="160">
          <template #default="{ row }">{{ row.verifyTime || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row._pending ? 'warning' : 'success'" size="small" round effect="light">
              {{ row._pending ? '待自提' : '已核销' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row._pending" type="primary" size="small" plain @click="prefillVerify(row)">扫码核销</el-button>
            <el-button v-else link size="small" @click="ElMessage.info('核销详情：' + row.orderNo + '（' + (row.verifier || '—') + '）')">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
v-model:current-page="recordQuery.page" v-model:page-size="recordQuery.size"
          layout="total, prev, pager, next" :total="recordTotal" @current-change="loadRecords" />
      </div>

      <div class="timeline-note">
        核销规则：买家到店出示动态核销码（每分钟刷新防截图盗用）→ 店员扫码 → 订单转「已完成」→ 库存扣减（自提订单下单时已预占）。核销记录不可删除，留痕可追溯。
      </div>
    </div>

    <!-- 门店编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑门店信息' : '新增门店'" width="460px" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="地址" required><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="营业时间">
          <el-input v-model="form.operatingHours" placeholder="如 09:00-22:00" />
          <div class="sub">契约 AC-12 门店字段无营业时间；后端不识别时该字段不生效，前端本地留存展示</div>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="OPEN">营业中</el-radio>
            <el-radio value="CLOSED">筹备中/停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStore">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listStores, createStore, updateStore, verifyOrder, listVerifyRecords } from '@/api/store'
import { listOrders } from '@/api/order'
import { fen2yuan } from '@/utils/format'

const STORE_HOURS_KEY = 'zl_store_operating_hours'

const stores = ref([])
const records = ref([])
const recordTotal = ref(0)
const recordQuery = reactive({ page: 1, size: 10, storeId: null })
const recordKeyword = ref('')
const recordDate = ref(null)
const verifyResult = ref(null)
const verifyForm = reactive({ orderNo: '', code: '', storeId: null })

const editVisible = ref(false)
const form = reactive({ id: null, name: '', address: '', phone: '', status: 'OPEN', operatingHours: '' })

// 营业时间：接口无该字段时读本地留存（按门店 id）
function loadHoursCache() {
  try {
    return JSON.parse(localStorage.getItem(STORE_HOURS_KEY)) || {}
  } catch {
    return {}
  }
}

function operatingHoursOf(row) {
  return row.operatingHours || loadHoursCache()[row.id] || '—'
}

// 合并展示：待自提订单（PICKUP_WAIT）+ 已核销记录
const displayVerifyRows = computed(() => {
  const keyword = recordKeyword.value.trim()
  const date = recordDate.value
  const rows = verifyMerged.value.filter((r) => {
    if (keyword) {
      const hay = `${r.orderNo || ''}${r.verifier || ''}${r.storeName || ''}${r.phoneTail || ''}`
      if (!hay.includes(keyword)) return false
    }
    if (date && r.verifyTime && !r.verifyTime.startsWith(date)) return false
    if (date && !r.verifyTime) return false
    return true
  })
  return rows
})

const verifyMerged = computed(() => {
  // 待自提订单排前（可核销），已核销记录随后
  return [...pendingPickups.value, ...records.value]
})

const pendingPickups = ref([])

async function loadPendingPickups() {
  try {
    const res = await listOrders({ status: 'PICKUP_WAIT', page: 1, size: 20 })
    pendingPickups.value = (res.rows || []).map((o) => ({
      id: 'p-' + o.orderNo,
      orderNo: o.orderNo,
      storeName: o.storeName || (o.store && o.store.name) || '—',
      _goods: (o.items && o.items[0]) ? { name: o.items[0].name, spec: [o.items[0].flavor, o.items[0].spec].filter(Boolean).join(' '), quantity: o.items[0].quantity } : null,
      payAmount: o.payAmount,
      verifyTime: null,
      _pending: true,
      phoneTail: o.phone || ''
    }))
  } catch {
    pendingPickups.value = []
  }
}

async function loadStores() {
  const res = await listStores()
  stores.value = (res.data || []).map((s, i) => ({ ...s, isMain: i === 0 }))
  if (!verifyForm.storeId && stores.value.length) verifyForm.storeId = stores.value[0].id
}

async function loadRecords() {
  const params = { page: recordQuery.page, size: recordQuery.size }
  if (recordQuery.storeId) params.storeId = recordQuery.storeId
  const res = await listVerifyRecords(params)
  records.value = (res.rows || []).map((r) => ({ ...r, _pending: false }))
  recordTotal.value = res.total
}

function applyRecordFilter() {
  // 前端过滤（computed displayVerifyRows 即时生效）
}

function prefillVerify(row) {
  verifyForm.orderNo = row.orderNo
  verifyResult.value = null
  ElMessage.info('已带入订单号，请输入买家出示的 6 位动态核销码')
}

async function doVerify() {
  if (!verifyForm.storeId) return ElMessage.warning('请选择门店')
  if (!verifyForm.orderNo) return ElMessage.warning('请输入订单号')
  if (!/^\d{6}$/.test(verifyForm.code)) return ElMessage.warning('请输入 6 位核销码')
  try {
    await verifyOrder({ ...verifyForm })
    verifyResult.value = { success: true }
    ElMessage.success('核销成功')
    verifyForm.code = ''
    loadRecords()
    loadPendingPickups()
  } catch (e) {
    verifyResult.value = { success: false, msg: e.message }
  }
}

function openEdit(row) {
  if (row) {
    Object.assign(form, { id: row.id, name: row.name, address: row.address, phone: row.phone, status: row.status, operatingHours: row.operatingHours || loadHoursCache()[row.id] || '' })
  } else {
    Object.assign(form, { id: null, name: '', address: '', phone: '', status: 'OPEN', operatingHours: '' })
  }
  editVisible.value = true
}

async function submitStore() {
  if (!form.name || !form.address) return ElMessage.warning('名称与地址必填')
  // operatingHours 契约未定义：一并提交（后端不识别即忽略），并本地留存保证展示
  const payload = { name: form.name, address: form.address, phone: form.phone, status: form.status, operatingHours: form.operatingHours || undefined }
  if (form.id) {
    await updateStore(form.id, payload)
    if (form.operatingHours) {
      const cache = loadHoursCache()
      cache[form.id] = form.operatingHours
      localStorage.setItem(STORE_HOURS_KEY, JSON.stringify(cache))
    }
    ElMessage.success('保存成功')
  } else {
    await createStore({ name: form.name, address: form.address, phone: form.phone, status: form.status })
    ElMessage.success('新增成功')
  }
  editVisible.value = false
  loadStores()
}

onMounted(() => {
  loadStores()
  loadRecords()
  loadPendingPickups()
})
</script>

<style scoped>
.verify-box {
  background: #fafbfc;
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}
.verify-box .lab {
  font-size: 12px;
  color: var(--text-2);
  flex: none;
}
</style>
