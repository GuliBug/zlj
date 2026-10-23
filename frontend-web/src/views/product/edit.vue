<template>
  <div class="panel-card">
    <h3 class="panel-title">{{ isEdit ? '编辑商品' : '新增 / 编辑商品' }}</h3>

    <!-- 三步步骤条 -->
    <el-steps :active="step" align-center finish-status="success" class="steps">
      <el-step title="① 基本信息" @click="step = 0" />
      <el-step title="② SKU 与价格" @click="step = 1" />
      <el-step title="③ 图片与详情" @click="step = 2" />
    </el-steps>

    <!-- 第①步 基本信息 -->
    <div v-show="step === 0">
      <el-form label-width="90px" class="step-form">
        <el-form-item label="商品名称" required>
          <div class="w-full">
            <el-input v-model="form.name" placeholder="如：六号究 · 正隆记门店同款" @input="onNameInput" :class="{ 'bad-input': nameHits.length }" />
            <div v-if="nameHits.length" class="err-text">命中违禁词 {{ nameHits.length }} 个：{{ nameHits.join('、') }}，保存被阻断</div>
          </div>
        </el-form-item>
        <el-form-item label="商品分类" required>
          <el-select v-model="form.categoryId" placeholder="请选择档位分类" style="width: 260px">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="品牌"><el-input v-model="form.brand" style="width: 260px" /></el-form-item>
        <el-form-item label="产地"><el-input v-model="form.origin" style="width: 260px" /></el-form-item>
      </el-form>
      <div class="step-nav">
        <el-button type="primary" @click="goStep(1)">下一步：SKU 与价格</el-button>
      </div>
    </div>

    <!-- 第②步 SKU 与价格 -->
    <div v-show="step === 1">
      <div class="spec-box">
        <span class="lab">快捷生成组合</span>
        <el-input v-model="tasteInput" style="width: 200px" placeholder="口味/系列（逗号分隔）如：六号究,九号究" size="small" />
        <span class="x">×</span>
        <el-input v-model="specInput" style="width: 200px" placeholder="规格（逗号分隔）如：30g×10包,30g×5包" size="small" />
        <el-button size="small" plain @click="genSkus">生成 SKU 组合</el-button>
        <div style="flex: 1"></div>
        <el-button size="small" plain @click="askBatch('price')">批量填价</el-button>
        <el-button size="small" plain @click="askBatch('stock')">批量填库存</el-button>
        <el-button size="small" plain @click="askBatch('alert')">批量填阈值</el-button>
        <el-button size="small" plain @click="genBarcodes">批量生成条码</el-button>
        <el-button size="small" plain @click="addSku">＋ 添加 SKU</el-button>
      </div>

      <el-table :data="form.skus" size="small">
        <el-table-column label="条码" width="160">
          <template #default="{ row }">
            <el-input v-model="row.barcode" size="small" placeholder="13 位条码" :class="{ 'bad-input': !String(row.barcode).trim() }" />
          </template>
        </el-table-column>
        <el-table-column label="规格（口味 + 规格）" min-width="170">
          <template #default="{ row }">
            <el-input v-model="row.specFull" size="small" placeholder="口味 + 规格" :class="{ 'bad-input': !String(row.specFull).trim() }" />
          </template>
        </el-table-column>
        <el-table-column label="售价(元)" width="110">
          <template #default="{ row }">
            <el-input-number
v-model="row.priceYuan" :min="0" :precision="2" :controls="false" size="small" style="width: 90px"
              :class="{ 'bad-input': !(row.priceYuan > 0) }" />
          </template>
        </el-table-column>
        <el-table-column label="成本价(元)" width="110">
          <template #default="{ row }">
            <el-input-number v-model="row.costYuan" :min="0" :precision="2" :controls="false" size="small" style="width: 90px" />
          </template>
        </el-table-column>
        <el-table-column label="库存" width="100">
          <template #default="{ row }">
            <el-input-number
v-model="row.stock" :min="0" :controls="false" size="small" style="width: 80px"
              :class="{ 'bad-input': row.stock == null || row.stock < 0 }" />
          </template>
        </el-table-column>
        <el-table-column label="安全阈值" width="100">
          <template #default="{ row }">
            <el-input-number v-model="row.threshold" :min="0" :controls="false" size="small" style="width: 80px" />
          </template>
        </el-table-column>
        <el-table-column label="库存货值" width="110">
          <template #default="{ row }"><b>¥{{ skuValue(row) }}</b></template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ $index }">
            <el-button link size="small" @click="dupSku($index)">复制</el-button>
            <el-button link size="small" type="danger" @click="delSku($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="sku-summary" v-if="form.skus.length">
        共 <b>{{ form.skus.length }}</b> 个 SKU · 总库存 <b>{{ totalStock }}</b> 件 · 库存货值合计 ¥{{ totalValue }}
        <template v-if="badSkuCount">
          · <span class="err-text-inline">⚠ {{ badSkuCount }} 行信息不完整，提交上架将被阻断</span>
        </template>
        <template v-else>· <span class="ok-text">✓ SKU 信息完整</span></template>
        <div class="sub">校验规则：条码必填且不可重复；规格必填；售价须大于 0（以分为最小单位存储）；库存 ≥ 0。改价仅对新订单生效。</div>
      </div>
      <div class="sku-summary" v-else>提交上架前必须至少 1 个 SKU。可用上方「生成 SKU 组合」按口味×规格批量生成，或逐个添加。</div>

      <div class="step-nav">
        <el-button @click="step = 0">上一步</el-button>
        <el-button type="primary" @click="goStep(2)">下一步：图片与详情</el-button>
      </div>
    </div>

    <!-- 第③步 图片与详情 -->
    <div v-show="step === 2">
      <el-form label-width="90px" class="step-form">
        <el-form-item label="主图 URL">
          <el-input v-model="form.imagesText" placeholder="多个 URL 用英文逗号分隔（示意图）" />
          <div class="img-preview">
            <el-image v-for="(img, i) in mainImages" :key="i" :src="img" fit="cover" class="upbox-img" :preview-src-list="mainImages" preview-teleported>
              <template #error>
                <img :src="PLACEHOLDER_IMG" class="upbox-img" style="width: 100%; height: 100%; object-fit: cover" />
              </template>
            </el-image>
            <div v-if="!mainImages.length" class="upbox">＋</div>
            <div v-if="mainImages.length < 3" class="upbox">＋</div>
          </div>
        </el-form-item>
        <el-form-item label="详情图 URL">
          <el-input v-model="form.detailImagesText" placeholder="多个 URL 用英文逗号分隔（示意图）" />
          <div class="img-preview">
            <el-image v-for="(img, i) in detailImages" :key="i" :src="img" fit="cover" class="upbox-img" :preview-src-list="detailImages" preview-teleported>
              <template #error>
                <img :src="PLACEHOLDER_IMG" class="upbox-img" style="width: 100%; height: 100%; object-fit: cover" />
              </template>
            </el-image>
            <div v-if="!detailImages.length" class="upbox">＋</div>
          </div>
        </el-form-item>
        <el-form-item label="商品描述">
          <div class="w-full">
            <el-input v-model="form.description" type="textarea" :rows="3" placeholder="纯描述性文案；禁止功效、提神、保健类宣传" @input="onDescInput" />
            <div v-if="descHits.length" class="err-text">命中违禁词 {{ descHits.length }} 个：{{ descHits.join('、') }}，保存被阻断</div>
          </div>
        </el-form-item>
      </el-form>
      <div class="step-nav">
        <el-button @click="step = 1">上一步</el-button>
      </div>
    </div>

    <!-- 底部操作 -->
    <div class="bottom-ops">
      <el-button plain @click="submit('OFF')">保存草稿</el-button>
      <el-button type="primary" :loading="saving" @click="submit('ON')">提交上架</el-button>
    </div>
    <div class="timeline-note">
      提交上架前系统自动执行：① 违禁词实时校验（功效类 + 广告法绝对化用语）→ ② 警示语自动植入（不可删除）→ ③ 修改售价弹窗确认「对新订单生效」。全部通过方可上架。
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProduct, createProduct, updateProduct, checkBannedWords, listCategories } from '@/api/product'
import { yuan2fen } from '@/utils/format'
import { PLACEHOLDER_IMG } from '@/utils/placeholder'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)

const step = ref(0)
const saving = ref(false)
const categories = ref([])

const form = reactive({
  id: null,
  name: '',
  categoryId: null,
  brand: '正隆記',
  origin: '湖南湘潭',
  description: '',
  imagesText: '',
  detailImagesText: '',
  skus: []
})

const tasteInput = ref('')
const specInput = ref('')
const nameHits = ref([])
const descHits = ref([])
let nameTimer = null
let descTimer = null

const mainImages = computed(() => form.imagesText.split(/[,，]/).map((s) => s.trim()).filter(Boolean))
const detailImages = computed(() => form.detailImagesText.split(/[,，]/).map((s) => s.trim()).filter(Boolean))

const totalStock = computed(() => form.skus.reduce((a, s) => a + (Number(s.stock) || 0), 0))
const totalValue = computed(() =>
  form.skus.reduce((a, s) => a + (Number(s.priceYuan) || 0) * (Number(s.stock) || 0), 0).toFixed(2)
)
const badSkuCount = computed(
  () => form.skus.filter((s) => !String(s.barcode).trim() || !String(s.specFull).trim() || !(s.priceYuan > 0) || !(s.stock >= 0)).length
)

function skuValue(row) {
  return ((Number(row.priceYuan) || 0) * (Number(row.stock) || 0)).toFixed(2)
}

function goStep(n) {
  if (n > 0 && !form.name) {
    ElMessage.warning('请先填写商品名称')
    step.value = 0
    return
  }
  step.value = n
}

// ===== 违禁词实时校验（oninput 防抖 400ms 调 POST /admin-api/check-banned-words）=====
function onNameInput() {
  clearTimeout(nameTimer)
  nameTimer = setTimeout(async () => {
    if (!form.name.trim()) {
      nameHits.value = []
      return
    }
    try {
      const res = await checkBannedWords(form.name)
      nameHits.value = res.data.hits || []
    } catch {
      /* 忽略：降级保持占位 */
    }
  }, 400)
}

function onDescInput() {
  clearTimeout(descTimer)
  descTimer = setTimeout(async () => {
    if (!form.description.trim()) {
      descHits.value = []
      return
    }
    try {
      const res = await checkBannedWords(form.description)
      descHits.value = res.data.hits || []
    } catch {
      /* 忽略：降级保持占位 */
    }
  }, 400)
}

// ===== SKU 操作 =====
function genSkus() {
  const tastes = tasteInput.value.split(/[,，]/).map((s) => s.trim()).filter(Boolean)
  const specs = specInput.value.split(/[,，]/).map((s) => s.trim()).filter(Boolean)
  if (!tastes.length || !specs.length) {
    ElMessage.warning('请填写口味与规格（逗号分隔多个，自动生成笛卡尔积）')
    return
  }
  let added = 0
  tastes.forEach((t) =>
    specs.forEach((z) => {
      const specFull = `${t} ${z}`
      if (!form.skus.some((s) => s.specFull === specFull)) {
        form.skus.push({ barcode: '', specFull, priceYuan: undefined, costYuan: undefined, stock: 0, threshold: 50 })
        added++
      }
    })
  )
  tasteInput.value = ''
  specInput.value = ''
  ElMessage.success(added ? `已生成 ${added} 个 SKU 组合（重复组合已自动跳过），请补全条码与价格` : '没有新增组合（均已存在）')
}

function addSku() {
  form.skus.push({ barcode: '', specFull: '', priceYuan: undefined, costYuan: undefined, stock: 0, threshold: 50 })
  ElMessage.info('已添加一行 SKU（空行提交会被阻断）')
}

function delSku(i) {
  form.skus.splice(i, 1)
  ElMessage.info('已删除该 SKU（保存后删除需二次确认并留审计日志）')
}

function dupSku(i) {
  const s = { ...form.skus[i] }
  s.barcode = ''
  form.skus.splice(i + 1, 0, s)
  ElMessage.info('已复制一行，条码已清空待填写（条码不可重复）')
}

const BATCH_CONF = {
  price: { title: '批量填充售价（元）', ph: '如 30.00，应用到所有 SKU 行' },
  stock: { title: '批量填充库存', ph: '如 100，应用到所有 SKU 行' },
  alert: { title: '批量填充安全阈值', ph: '如 50，应用到所有 SKU 行' }
}

async function askBatch(field) {
  if (!form.skus.length) return ElMessage.warning('请先添加 SKU')
  try {
    const { value } = await ElMessageBox.prompt(BATCH_CONF[field].ph, BATCH_CONF[field].title, {
      inputPattern: /^\d+(\.\d+)?$/,
      inputErrorMessage: '请输入有效数字'
    })
    const v = Number(value)
    form.skus.forEach((s) => {
      if (field === 'price') s.priceYuan = v
      else if (field === 'stock') s.stock = v
      else s.threshold = v
    })
    ElMessage.success(`已批量填充 ${form.skus.length} 行（批量操作留审计日志）`)
  } catch {
      /* 忽略：降级保持占位 */
    }
}

let barcodeSeq = 6901234500001
function genBarcodes() {
  if (!form.skus.length) return ElMessage.warning('请先添加 SKU')
  form.skus.forEach((s) => {
    if (!String(s.barcode).trim()) s.barcode = String(barcodeSeq++)
  })
  ElMessage.success('已为空条码的 SKU 生成占位条码（正式接入一品一码后由系统分配）')
}

// ===== 提交 =====
async function submit(status) {
  const isOnshelf = status === 'ON'
  if (!form.name.trim()) return ElMessage.warning('请填写商品名称')
  if (nameHits.value.length) return ElMessage.error('商品名称命中违禁词，保存被阻断')
  if (descHits.value.length) return ElMessage.error('商品描述命中违禁词，保存被阻断')
  if (!form.categoryId) return ElMessage.warning('请选择商品分类')
  if (!form.skus.length) return ElMessage.warning('请至少添加 1 个 SKU')

  if (isOnshelf) {
    if (badSkuCount.value) return ElMessage.error(`有 ${badSkuCount.value} 行 SKU 信息不完整（条码/规格/售价/库存），已标红`)
    const barcodes = form.skus.map((s) => String(s.barcode).trim())
    if (new Set(barcodes).size !== barcodes.length) return ElMessage.error('存在重复条码，请检查')
    // 提交上架前：改价确认（对未支付订单不生效）
    try {
      await ElMessageBox.confirm(
        `提交上架「${form.name}」共 ${form.skus.length} 个 SKU。\n提示：警示语将由系统自动植入（不可删除）；改价仅对新订单生效，对未支付订单不生效。`,
        '提交上架确认',
        { type: 'warning', customStyle: { whiteSpace: 'pre-line' } }
      )
    } catch {
      return
    }
  }

  saving.value = true
  try {
    const payload = {
      name: form.name,
      categoryId: form.categoryId,
      brand: form.brand,
      origin: form.origin,
      status,
      description: form.description,
      images: mainImages.value,
      detailImages: detailImages.value,
      skus: form.skus.map((s) => {
        const full = String(s.specFull || '').trim()
        const flavor = full.split(/\s+/)[0] || ''
        const spec = full.split(/\s+/).slice(1).join(' ') || full
        return {
          ...(s.id ? { id: s.id } : {}),
          flavor,
          spec,
          barcode: String(s.barcode).trim(),
          price: yuan2fen(s.priceYuan || 0),
          costPrice: yuan2fen(s.costYuan || 0),
          stock: Number(s.stock) || 0
        }
      })
    }
    if (isEdit.value) {
      await updateProduct(form.id, payload)
      ElMessage.success('保存成功（变更已留审计）')
    } else {
      await createProduct(payload)
      ElMessage.success(isOnshelf ? '已提交上架，H5 端即时可见' : '草稿已保存')
    }
    router.push('/products')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const res = await listCategories()
  categories.value = res.data || []
  if (isEdit.value) {
    const res2 = await getProduct(route.params.id)
    const d = res2.data || {}
    Object.assign(form, {
      id: d.id,
      name: d.name,
      categoryId: d.categoryId,
      brand: d.brand,
      origin: d.origin,
      description: d.description || '',
      imagesText: (d.images || []).join(','),
      detailImagesText: (d.detailImages || []).join(',')
    })
    form.skus = (d.skus || []).map((s) => ({
      id: s.id,
      barcode: s.barcode || '',
      specFull: [s.flavor, s.spec].filter(Boolean).join(' '),
      priceYuan: s.price != null ? s.price / 100 : undefined,
      costYuan: s.costPrice != null ? s.costPrice / 100 : undefined,
      stock: s.stock ?? 0,
      threshold: s.threshold ?? 50
    }))
  }
})
</script>

<style scoped>
.steps {
  margin-bottom: 18px;
}
.steps :deep(.el-step__title) {
  cursor: pointer;
  font-size: 13px;
}
.step-form {
  max-width: 720px;
}
.w-full {
  width: 100%;
}
.err-text {
  color: var(--primary);
  font-size: 11px;
  margin-top: 4px;
}
.err-text-inline {
  color: var(--primary);
}
.ok-text {
  color: var(--green);
}
.bad-input :deep(.el-input__wrapper),
.bad-input :deep(.el-input-number .el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--primary) inset;
}
.spec-box {
  background: #fafbfc;
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 10px;
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}
.spec-box .lab {
  font-size: 12px;
  color: var(--text-2);
  flex: none;
}
.spec-box .x {
  color: #c9cdd4;
}
.sku-summary {
  font-size: 12px;
  color: var(--text-2);
  margin-top: 8px;
  line-height: 1.8;
}
.img-preview {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.upbox {
  width: 60px;
  height: 60px;
  border: 1px dashed #c9cdd4;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c9cdd4;
  font-size: 22px;
}
.upbox:hover {
  border-color: var(--primary);
  color: var(--primary);
}
.upbox-img {
  width: 60px;
  height: 60px;
  border-radius: 8px;
}
.step-nav {
  margin-top: 18px;
  display: flex;
  gap: 10px;
}
.bottom-ops {
  margin-top: 18px;
  display: flex;
  gap: 10px;
}
</style>
