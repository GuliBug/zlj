<template>
  <div class="panel-card">
    <h3 class="panel-title">审计与告警 <span class="sub" style="font-weight: 400">（从工作台「资损监控」进入）</span></h3>
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <!-- 审计日志 -->
        <el-tab-pane label="审计日志" name="audit">
          <div class="filter-bar">
            <el-select v-model="logQuery.module" placeholder="全部模块" clearable filterable allow-create style="width: 180px">
              <el-option v-for="m in moduleOptions" :key="m" :label="m" :value="m" />
            </el-select>
            <el-button type="primary" @click="loadLogs(true)">查询</el-button>
          </div>
          <el-table :data="logRows" border stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="operator" label="操作人" width="110" />
            <el-table-column prop="module" label="模块" width="120" />
            <el-table-column prop="action" label="动作" width="140" show-overflow-tooltip />
            <el-table-column prop="detail" label="变更前后值" min-width="260" show-overflow-tooltip />
            <el-table-column prop="ip" label="IP" width="130" />
            <el-table-column prop="createdAt" label="时间" width="170" />
          </el-table>
          <div class="pager">
            <el-pagination
v-model:current-page="logQuery.page" v-model:page-size="logQuery.size"
              layout="total, prev, pager, next" :total="logTotal" @current-change="loadLogs" />
          </div>
        </el-tab-pane>

        <!-- 告警列表 -->
        <el-tab-pane label="告警列表" name="alerts">
          <div class="timeline-note" style="margin: 0 0 12px">
            资损监控范围：重复支付 / 金额异常 / 状态回跳 / 对账差异超 24h 未处理 / 库存预警 → 告警推送财务+超管（30 分钟未响应升级短信）。
          </div>
          <div class="filter-bar">
            <el-button type="primary" @click="loadAlerts">刷新</el-button>
            <el-tag size="small" type="info">类型：重复支付 / 异常金额 / 状态回滚 / 对账差异 / 库存预警</el-tag>
          </div>
          <el-table :data="alertRows" border stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column label="类型" width="160">
              <template #default="{ row }">
                <el-tag :type="alertTag(row.type)" size="small">{{ alertText(row.type) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="content" label="内容" min-width="320" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="110" />
            <el-table-column prop="createdAt" label="时间" width="170" />
          </el-table>
          <div class="pager">
            <el-pagination
v-model:current-page="alertQuery.page" v-model:page-size="alertQuery.size"
              layout="total, prev, pager, next" :total="alertTotal" @current-change="loadAlerts" />
          </div>
        </el-tab-pane>
      </el-tabs>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { listAuditLogs, listAlerts } from '@/api/audit'

const activeTab = ref('audit')

const moduleOptions = ['PRODUCT', 'STOCK', 'ORDER', 'AFTERSALE', 'MEMBER', 'LEVEL_RULE', 'VERIFY', 'ALERT']

const logQuery = reactive({ page: 1, size: 10, module: null })
const logRows = ref([])
const logTotal = ref(0)

const alertQuery = reactive({ page: 1, size: 10 })
const alertRows = ref([])
const alertTotal = ref(0)

function onTabChange(tab) {
  if (tab === 'alerts') loadAlerts()
  if (tab === 'audit') loadLogs(true)
}

function alertText(type) {
  return { DUPLICATE_PAY: '重复支付', ABNORMAL_AMOUNT: '异常金额', STATUS_ROLLBACK: '状态回滚', RECON_DIFF: '对账差异', STOCK_ALERT: '库存预警' }[type] || type
}

function alertTag(type) {
  return { DUPLICATE_PAY: 'danger', ABNORMAL_AMOUNT: 'danger', STATUS_ROLLBACK: 'warning', RECON_DIFF: 'warning', STOCK_ALERT: 'info' }[type] || 'info'
}

async function loadLogs(reset) {
  if (reset) logQuery.page = 1
  const params = { page: logQuery.page, size: logQuery.size }
  if (logQuery.module) params.module = logQuery.module
  const res = await listAuditLogs(params)
  logRows.value = res.rows
  logTotal.value = res.total
}

async function loadAlerts() {
  const res = await listAlerts({ page: alertQuery.page, size: alertQuery.size })
  alertRows.value = res.rows
  alertTotal.value = res.total
}

onMounted(() => loadLogs())
</script>
