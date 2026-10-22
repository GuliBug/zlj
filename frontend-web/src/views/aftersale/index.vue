<template>
  <div class="panel-card">
    <h3 class="panel-title">
      售后工单
      <span class="sub" style="font-weight: 400">（超 48h 未处理自动升级提醒管理员）</span>
    </h3>

    <!-- 筛选 -->
    <div class="filter-bar">
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 160px">
        <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
      <el-checkbox v-model="query.overdue" label="仅看超 48h 未审核" @change="load(true)" />
      <el-button @click="load(true)">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="rows" :row-class-name="overdueRowClass">
      <el-table-column label="工单号" min-width="170">
        <template #default="{ row }">
          {{ row.afterSaleNo }}
          <div v-if="row.auditOverdue" class="overdue-text">超 48h ⚠</div>
        </template>
      </el-table-column>
      <el-table-column prop="orderNo" label="订单号" min-width="170" show-overflow-tooltip />
      <el-table-column label="类型 / 原因" min-width="170">
        <template #default="{ row }">
          {{ row.typeText || row.type }}
          <div class="sub">{{ row.reason || '—' }}<template v-if="certCount(row)"> · 凭证 {{ certCount(row) }} 张</template></div>
        </template>
      </el-table-column>
      <el-table-column label="退款金额" width="150">
        <template #default="{ row }">
          <b>{{ fen2yuan(row.refundAmount) }}</b>
          <div class="sub">服务端计算 · 原路退回</div>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="申请时间" width="170" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small" round effect="light">{{ row.statusText || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING_AUDIT'" type="primary" size="small" plain @click="openAudit(row)">审核</el-button>
          <el-button v-if="row.status === 'REFUNDING'" type="primary" size="small" plain @click="doRefund(row)">退款</el-button>
          <el-button v-if="row.status === 'WAIT_RETURN'" size="small" plain @click="doConfirmReturn(row)">确认收货</el-button>
          <el-button v-if="row.status === 'REFUNDED'" link size="small" @click="openDetail(row)">退款流水</el-button>
          <el-button link size="small" @click="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager">
      <el-pagination
v-model:current-page="query.page" v-model:page-size="query.size"
        layout="total, prev, pager, next" :total="total" @current-change="load" />
    </div>

    <div class="timeline-note">
      每笔售后单全程留痕：状态每流转一步即生成流转记录（时间 / 操作方 / 动作 / 备注），商家端与买家 H5 端均可查看，记录不可删改；审核超 48h 自动升级提醒。
    </div>
    <div class="timeline-note">
      资金底线：退款幂等（重复点击不重复退款）→ 渠道受理失败自动重试 3 次（间隔 10 分钟）→ 仍失败挂起转财务人工。退款成功以渠道回调确认为准，回补库存可配置。
    </div>

    <!-- 详情抽屉（含流转时间线） -->
    <el-drawer v-model="detailVisible" title="售后详情 · 流转记录" size="480px" destroy-on-close>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="售后单号">{{ detail.afterSaleNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="订单号">{{ detail.orderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ detail.typeText || detail.type || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.statusText || detail.status || '-' }}</el-descriptions-item>
        <el-descriptions-item label="退款金额（服务端计算·原路退回）">{{ fen2yuan(detail.refundAmount) }}</el-descriptions-item>
        <el-descriptions-item label="申请原因">{{ detail.reason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="问题描述">{{ detail.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ detail.createdAt || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="drawer-section">凭证</div>
      <div class="img-box">
        <template v-if="detailImages.length">
          <el-image v-for="(img, i) in detailImages" :key="i" :src="img" fit="cover" class="cert-img" :preview-src-list="detailImages" preview-teleported />
        </template>
        <span v-else class="sub">无凭证</span>
      </div>

      <div class="drawer-section">流转时间线（不可删改）</div>
      <el-timeline v-if="(detail.flow || []).length">
        <el-timeline-item v-for="(f, i) in detail.flow" :key="i" :timestamp="f.time" :type="i === detail.flow.length - 1 ? 'primary' : ''">
          <b>{{ f.action }}</b>
          <span class="sub" style="margin-left: 8px">{{ f.operator }}</span>
          <div v-if="f.remark" class="sub" style="font-size: 12px">{{ f.remark }}</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无流转记录" />
    </el-drawer>

    <!-- 审核弹窗：意见必填，通过并退款 / 拒绝 -->
    <el-dialog v-model="auditVisible" title="售后审核" width="440px" destroy-on-close>
      <p class="audit-body">{{ auditBody }}</p>
      <el-form label-width="90px">
        <el-form-item label="审核意见" required>
          <el-input v-model="auditForm.opinion" type="textarea" :rows="3" placeholder="请填写审核意见（必填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="danger" plain @click="submitAudit(false)">拒绝</el-button>
        <el-button type="primary" @click="submitAudit(true)">通过并退款</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAftersales, getAftersale, auditAftersale, confirmReturn, refundAftersale } from '@/api/aftersale'
import { fen2yuan } from '@/utils/format'

const statusOptions = [
  { value: 'PENDING_AUDIT', label: '待审核' },
  { value: 'WAIT_RETURN', label: '待买家寄回' },
  { value: 'REFUNDING', label: '退款中' },
  { value: 'REFUNDED', label: '已退款' },
  { value: 'REJECTED', label: '已拒绝' }
]

const query = reactive({ page: 1, size: 10, status: null, overdue: false })
const rows = ref([])
const total = ref(0)

const detailVisible = ref(false)
const detail = ref({})
const detailImages = computed(() => detail.value.images || detail.value.certImages || [])

const auditVisible = ref(false)
const auditForm = reactive({ afterSaleNo: '', approve: true, opinion: '', row: null })
const auditBody = ref('')

function statusTag(status) {
  return { PENDING_AUDIT: 'warning', WAIT_RETURN: 'warning', REFUNDING: 'warning', REFUNDED: 'success', REJECTED: 'danger' }[status] || 'info'
}

function certCount(row) {
  const n = row.certCount ?? (row.images && row.images.length)
  return n || 0
}

function overdueRowClass({ row }) {
  return row.auditOverdue ? 'overdue-row' : ''
}

async function load(reset) {
  if (reset) query.page = 1
  const params = { page: query.page, size: query.size }
  if (query.status) params.status = query.status
  if (query.overdue) params.overdue = true
  const res = await listAftersales(params)
  rows.value = res.rows
  total.value = res.total
}

function resetQuery() {
  Object.assign(query, { page: 1, status: null, overdue: false })
  load()
}

async function openDetail(row) {
  const res = await getAftersale(row.afterSaleNo)
  detail.value = res.data || {}
  detailVisible.value = true
}

function openAudit(row) {
  Object.assign(auditForm, { afterSaleNo: row.afterSaleNo, opinion: '', row })
  auditBody.value =
    `工单 ${row.afterSaleNo} ｜ 订单 ${row.orderNo} ｜ ${row.typeText || row.type}${row.reason ? ' · ' + row.reason : ''}。\n` +
    `通过后：仅退款单直接触发原路退款 ${fen2yuan(row.refundAmount)}（幂等）；退货退款单进入待买家寄回，确认收货后退款并回补库存。`
  auditVisible.value = true
}

async function submitAudit(approve) {
  if (!auditForm.opinion.trim()) return ElMessage.warning('审核意见必填')
  await auditAftersale(auditForm.afterSaleNo, { approve, opinion: auditForm.opinion })
  ElMessage.success(approve ? '已通过：流转记录已追加（买家 H5 端同步可见）' : '已拒绝：流转记录已追加')
  auditVisible.value = false
  load()
}

// 确认收货（退货退款态，触发退款+库存回补）
async function doConfirmReturn(row) {
  try {
    await ElMessageBox.confirm(`确认已收到「${row.afterSaleNo}」的退货吗？确认后触发退款与库存回补。`, '二次确认', { type: 'warning' })
  } catch {
    return
  }
  await confirmReturn(row.afterSaleNo)
  ElMessage.success('已确认收货并触发退款')
  load()
}

// 退款（REFUNDING 态，后端幂等）
async function doRefund(row) {
  try {
    await ElMessageBox.confirm(
      `确定对「${row.afterSaleNo}」执行退款吗？金额 ${fen2yuan(row.refundAmount)}。\n（退款为幂等操作，重复调用不会重复退款）`,
      '退款二次确认',
      { type: 'error', confirmButtonText: '确认退款' }
    )
  } catch {
    return
  }
  await refundAftersale(row.afterSaleNo)
  ElMessage.success('退款已执行，以渠道回调确认为准')
  load()
}

onMounted(() => load())
</script>

<style scoped>
.overdue-text {
  color: var(--primary);
  font-size: 11px;
}
.drawer-section {
  font-weight: bold;
  margin: 16px 0 8px;
  color: var(--primary);
}
.img-box {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.cert-img {
  width: 90px;
  height: 90px;
  border-radius: 4px;
  border: 1px solid var(--line);
}
.audit-body {
  font-size: 13px;
  color: var(--text-2);
  line-height: 1.7;
  margin: 0 0 16px;
  white-space: pre-line;
}
</style>
