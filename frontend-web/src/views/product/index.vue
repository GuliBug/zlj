<template>
  <div class="panel-card">
    <h3 class="panel-title">
      商品列表
      <span class="r">
        <el-button type="primary" size="small" @click="$router.push('/products/new')">＋ 新增商品</el-button>
      </span>
    </h3>

    <!-- 工具栏 -->
    <div class="filter-bar">
      <el-input v-model="query.keyword" placeholder="商品名称 / 条码" clearable style="width: 200px" @keyup.enter="load(true)" />
      <el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width: 150px">
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px">
        <el-option label="上架中" value="ON" />
        <el-option label="已下架" value="OFF" />
      </el-select>
      <el-button @click="load(true)">查询</el-button>
      <div style="flex: 1"></div>
      <el-button plain :disabled="!selection.length" @click="batchChange('ON')">批量上架</el-button>
      <el-button plain :disabled="!selection.length" @click="batchChange('OFF')">批量下架</el-button>
      <el-button plain @click="exportCsv">导出</el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="rows" @selection-change="(s) => (selection = s)">
      <el-table-column type="selection" width="42" />
      <el-table-column label="商品" min-width="240">
        <template #default="{ row }">
          <div class="gname">
            <el-image :src="row.image || PLACEHOLDER_IMG" fit="cover" class="gthumb" :preview-src-list="[row.image || PLACEHOLDER_IMG]" preview-teleported>
              <template #error>
                <img :src="PLACEHOLDER_IMG" class="gthumb" style="width: 100%; height: 100%; object-fit: cover" />
              </template>
            </el-image>
            <div>
              {{ row.name }}
              <div class="sub">{{ (row.skus && row.skus.length) || 0 }} 个 SKU</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="categoryName" label="分类" width="110" />
      <el-table-column label="价格区间" width="150">
        <template #default="{ row }">{{ fen2yuan(row.priceMin) }} ~ {{ fen2yuan(row.priceMax) }}</template>
      </el-table-column>
      <el-table-column label="库存合计" width="100">
        <template #default="{ row }">
          <b :class="{ orange: stockSum(row) < 50 }">{{ stockSum(row) }}</b>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ON' ? 'success' : 'info'" size="small" round effect="light">
            {{ row.status === 'ON' ? '上架中' : '已下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="上下架" width="90">
        <template #default="{ row }">
          <el-switch
            :model-value="row.status === 'ON'"
            inline-prompt
            active-text="上架"
            inactive-text="下架"
            @change="(v) => toggleStatus(row, v)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push(`/products/${row.id}/edit`)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pager">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        layout="total, prev, pager, next"
        :total="total"
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listProducts, changeProductStatus, listCategories } from '@/api/product'
import { fen2yuan, downloadCsv } from '@/utils/format'
import { PLACEHOLDER_IMG } from '@/utils/placeholder'

const query = reactive({ page: 1, size: 10, categoryId: null, status: null, keyword: '' })
const rows = ref([])
const total = ref(0)
const categories = ref([])
const selection = ref([])

function stockSum(row) {
  return (row.skus || []).reduce((a, s) => a + (s.stock || 0), 0)
}

async function load(reset) {
  if (reset) query.page = 1
  const params = { page: query.page, size: query.size }
  if (query.categoryId) params.categoryId = query.categoryId
  if (query.status) params.status = query.status
  if (query.keyword) params.keyword = query.keyword
  const res = await listProducts(params)
  rows.value = res.rows
  total.value = res.total
}

async function toggleStatus(row, on) {
  const action = on ? '上架' : '下架'
  try {
    await ElMessageBox.confirm(`确定要${action}商品「${row.name}」吗？该操作将留审计记录。`, '二次确认', { type: 'warning' })
  } catch {
    return
  }
  await changeProductStatus([row.id], on ? 'ON' : 'OFF')
  ElMessage.success(action + '成功，变更已留审计')
  load()
}

async function batchChange(status) {
  const ids = selection.value.map((r) => r.id)
  const action = status === 'ON' ? '上架' : '下架'
  try {
    await ElMessageBox.confirm(`确定批量${action}选中的 ${ids.length} 个商品吗？该操作将留审计记录。`, '二次确认', { type: 'warning' })
  } catch {
    return
  }
  await changeProductStatus(ids, status)
  ElMessage.success('批量' + action + '成功')
  load()
}

// 契约未提供商品导出端点，前端按当前筛选结果生成 CSV
async function exportCsv() {
  const res = await listProducts({ ...query, page: 1, size: 1000 })
  const list = res.rows || []
  const header = 'ID,名称,分类,SKU数,价格区间(元),库存合计,状态'
  const lines = list.map((r) =>
    [r.id, r.name, r.categoryName || '', (r.skus || []).length, `${(r.priceMin / 100).toFixed(2)}~${(r.priceMax / 100).toFixed(2)}`, stockSum(r), r.status === 'ON' ? '上架中' : '已下架'].join(',')
  )
  downloadCsv([header, ...lines].join('\n'), `商品导出_${Date.now()}.csv`)
  ElMessage.info('说明：契约未提供商品导出接口，此处为前端按当前筛选结果生成 CSV')
}

onMounted(async () => {
  const catRes = await listCategories()
  categories.value = catRes.data || []
  load()
})
</script>

<style scoped>
.gname {
  display: flex;
  align-items: center;
  gap: 10px;
}
.gthumb {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  flex: none;
}
.orange {
  color: var(--orange);
}
</style>
