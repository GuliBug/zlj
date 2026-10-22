<template>
  <div class="panel-card">
    <h3 class="panel-title">
      会员管理
      <span class="r">
        <el-button size="small" plain @click="exportTip">导出（脱敏）</el-button>
        <el-button size="small" plain @click="openRules">等级规则设置</el-button>
      </span>
    </h3>

    <!-- 等级分布卡（人数从列表数据统计） -->
    <div class="cards">
      <div class="stat"><span>V0 访客</span><b>{{ levelCount('V0') }}</b><span>人</span></div>
      <div class="stat"><span>V1 银卡掌柜（≥500）</span><b>{{ levelCount('V1') }}</b><span>人</span></div>
      <div class="stat"><span>V2 金卡掌柜（≥2000）</span><b>{{ levelCount('V2') }}</b><span>人</span></div>
      <div class="stat"><span>V3 钻卡掌柜（≥5000）</span><b>{{ levelCount('V3') }}</b><span>人</span></div>
    </div>

    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="手机号（脱敏）" clearable style="width: 180px" @keyup.enter="load(true)" />
      <el-select v-model="levelFilter" placeholder="全部等级" clearable style="width: 140px">
        <el-option v-for="l in ['V0', 'V1', 'V2', 'V3']" :key="l" :label="l" :value="l" />
      </el-select>
      <el-button @click="load(true)">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
    </div>

    <el-table :data="displayRows">
      <el-table-column label="会员" width="140">
        <template #default="{ row }">{{ row.phone || row.id }}</template>
      </el-table-column>
      <el-table-column label="等级" width="140">
        <template #default="{ row }">
          <el-tag :type="levelTag(row.levelCode)" size="small" round effect="light">{{ levelText(row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="points" label="积分" width="100" />
      <el-table-column label="累计消费" width="120">
        <template #default="{ row }">{{ fen2yuan(row.totalPaid) }}</template>
      </el-table-column>
      <el-table-column label="常购门店" width="140">
        <template #default="{ row }">{{ row.storeName || row.frequentStore || '—' }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" width="170" />
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openAdjust(row)">调整积分</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager">
      <el-pagination
v-model:current-page="query.page" v-model:page-size="query.size"
        layout="total, prev, pager, next" :total="total" @current-change="load" />
    </div>

    <div class="timeline-note">
      等级规则（可配置，管理员修改留审计日志）：累计实付 ≥500 → V1；≥2000 → V2；≥5000 → V3。积分：实付 1 元 = 1 积分，退款按比例扣减；100 积分抵 1 元，单笔抵扣上限 5%。禁止成瘾性/诱导性玩法。
    </div>

    <!-- 等级规则编辑弹窗（GET/PUT /admin-api/level-rules，修改留审计） -->
    <el-dialog v-model="rulesVisible" title="会员等级规则设置" width="520px" destroy-on-close>
      <el-alert type="info" :closable="false" style="margin-bottom: 12px" title="阈值变更将写入审计日志（操作人/时间/变更前后值）" />
      <el-table :data="ruleRows" size="small">
        <el-table-column prop="levelName" label="等级" width="150" />
        <el-table-column label="升级阈值（累计实付，元）">
          <template #default="{ row }">
            <el-input-number v-model="row.thresholdYuan" :min="0" :precision="2" :controls="false" style="width: 160px" />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="rulesVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitRules">保存（留审计）</el-button>
      </template>
    </el-dialog>

    <!-- 调整积分弹窗（双重确认） -->
    <el-dialog v-model="adjustVisible" title="调整积分" width="440px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="会员"><span>{{ adjustRow.phone }}（{{ adjustRow.levelName || adjustRow.levelCode }}）</span></el-form-item>
        <el-form-item label="变动值" required>
          <el-input-number v-model="adjustForm.change" :step="10" style="width: 160px" />
          <span class="sub" style="margin-left: 8px">支持 ±（正数增加/负数扣减）</span>
        </el-form-item>
        <el-form-item label="原因" required><el-input v-model="adjustForm.reason" type="textarea" :rows="2" placeholder="必填" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdjust">提交（双重确认）</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listMembers, adjustPoints, getLevelRules, updateLevelRules } from '@/api/member'
import { fen2yuan } from '@/utils/format'

const LEVEL_TEXT = { V0: 'V0 访客', V1: 'V1 银卡掌柜', V2: 'V2 金卡掌柜', V3: 'V3 钻卡掌柜' }

const query = reactive({ page: 1, size: 10, keyword: '' })
const rows = ref([])
const total = ref(0)
const saving = ref(false)
const levelFilter = ref(null)

// 等级分布统计：取全量（脱敏）列表数据统计，避免只统计当前页
const statRows = ref([])

const rulesVisible = ref(false)
const ruleRows = ref([])

const adjustVisible = ref(false)
const adjustRow = reactive({ id: null, phone: '', levelName: '', levelCode: '' })
const adjustForm = reactive({ change: 0, reason: '' })

// 等级筛选为前端过滤
const displayRows = computed(() => {
  if (!levelFilter.value) return rows.value
  return rows.value.filter((r) => (r.levelCode || '') === levelFilter.value)
})

function levelText(row) {
  return row.levelName || LEVEL_TEXT[row.levelCode] || row.levelCode || '—'
}

function levelTag(code) {
  return { V0: 'info', V1: 'warning', V2: 'success', V3: 'danger' }[code] || 'info'
}

function levelCount(code) {
  return statRows.value.filter((r) => (r.levelCode || '') === code).length
}

async function load(reset) {
  if (reset) query.page = 1
  const params = { page: query.page, size: query.size }
  if (query.keyword) params.keyword = query.keyword
  const res = await listMembers(params)
  rows.value = res.rows
  total.value = res.total
}

async function loadStats() {
  try {
    const res = await listMembers({ page: 1, size: 1000 })
    statRows.value = res.rows || []
  } catch {
    statRows.value = []
  }
}

function resetQuery() {
  Object.assign(query, { page: 1, keyword: '' })
  levelFilter.value = null
  load()
}

function exportTip() {
  ElMessage.info('导出为脱敏数据（契约未提供导出端点，可后续扩展）')
}

async function openRules() {
  const res = await getLevelRules()
  ruleRows.value = (res.data || []).map((r) => ({ ...r, thresholdYuan: r.threshold != null ? r.threshold / 100 : 0 }))
  rulesVisible.value = true
}

async function submitRules() {
  saving.value = true
  try {
    await updateLevelRules(ruleRows.value.map((r) => ({ levelCode: r.levelCode, threshold: Math.round(r.thresholdYuan * 100) })))
    ElMessage.success('等级规则已保存（变更留审计）')
    rulesVisible.value = false
  } finally {
    saving.value = false
  }
}

function openAdjust(row) {
  Object.assign(adjustRow, { id: row.id, phone: row.phone, levelName: row.levelName, levelCode: row.levelCode })
  adjustForm.change = 0
  adjustForm.reason = ''
  adjustVisible.value = true
}

// 双重确认（契约 AC-15：双重确认由前端承担）
async function submitAdjust() {
  if (!adjustForm.change) return ElMessage.warning('变动值不能为 0')
  if (!adjustForm.reason.trim()) return ElMessage.warning('原因必填')
  const action = adjustForm.change > 0 ? '增加' : '扣减'
  try {
    await ElMessageBox.confirm(
      `第一次确认：对会员 ${adjustRow.phone} ${action} ${Math.abs(adjustForm.change)} 积分，原因「${adjustForm.reason}」。确定吗？`,
      '积分调整确认（1/2）',
      { type: 'warning' }
    )
    await ElMessageBox.confirm(
      `第二次确认：积分调整将留审计且影响会员权益，确定执行「${action} ${Math.abs(adjustForm.change)} 积分」吗？`,
      '积分调整确认（2/2）',
      { type: 'error', confirmButtonText: '最终确认' }
    )
  } catch {
    return
  }
  await adjustPoints(adjustRow.id, { change: adjustForm.change, reason: adjustForm.reason })
  ElMessage.success('积分调整成功（留审计）')
  adjustVisible.value = false
  load()
  loadStats()
}

onMounted(() => {
  load()
  loadStats()
})
</script>

<style scoped>
.cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 14px;
}
.stat {
  background: var(--card);
  border-radius: var(--radius);
  border: 1px solid var(--line);
  padding: 14px 16px;
}
.stat b {
  font-size: 22px;
  display: block;
  margin: 4px 0 2px;
}
.stat > span {
  color: var(--text-2);
  font-size: 12px;
}
</style>
