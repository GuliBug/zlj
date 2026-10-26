<template>
  <div v-if="d">
    <div class="navbar-plain"><button class="back" @click="goBack">‹</button><div class="title">售后详情</div></div>

    <!-- 工单信息 -->
    <div class="card">
      <div style="font-family:var(--kai);font-size:15px;letter-spacing:1px">工单 {{ d.afterSaleNo }}</div>
      <div style="font-size:12px;line-height:1.9;margin-top:6px">
        售后类型：{{ d.typeText }}<br>
        关联订单：{{ d.orderNo }}<br>
        退款金额：<span class="price">¥{{ yuan(d.refundAmount) }}</span><br>
        申请时间：{{ d.createdAt }}<br>
        当前状态：<span class="status">{{ d.statusText }}</span>
      </div>
    </div>

    <!-- 申请凭证 -->
    <div class="card" v-if="proofImgs.length">
      <div class="card-title">申请凭证</div>
      <div class="proof-row">
        <img v-for="(src, i) in proofImgs" :key="i" :src="src" class="proof-img" @click="preview(src)">
      </div>
    </div>

    <!-- 寄回物流（已回填） -->
    <div class="card" v-if="d.returnCompany || d.returnNo">
      <div class="card-title">寄回物流</div>
      <div style="font-size:12px;line-height:1.9">{{ d.returnCompany }} · {{ d.returnNo }}</div>
    </div>

    <!-- 填写寄回物流（退货退款待寄回态） -->
    <div class="card" v-if="canReturn">
      <div class="card-title">填写寄回物流</div>
      <div class="field">
        <label>物流公司 *</label>
        <input v-model.trim="retForm.company" placeholder="如：顺丰速运" />
      </div>
      <div class="field">
        <label>运单号 *</label>
        <input v-model.trim="retForm.no" placeholder="请输入寄回运单号" />
      </div>
      <button class="btn" style="width:100%;margin-top:4px" @click="submitLogistics">提交寄回物流</button>
      <div class="minor" style="margin-top:8px">提交后商家确认收货即触发退款</div>
    </div>

    <!-- 流转记录时间线 -->
    <div class="card">
      <div class="card-title">流转记录</div>
      <div class="timeline" v-if="d.flow && d.flow.length">
        <div v-for="(f, i) in d.flow" :key="i" class="timeline-item">
          <div class="t-action">{{ f.action }}</div>
          <div class="t-time">{{ f.time }}<template v-if="f.operator"> · {{ f.operator }}</template></div>
          <div class="t-remark" v-if="f.remark">{{ f.remark }}</div>
        </div>
      </div>
      <div class="muted" v-else>暂无流转记录</div>
      <div style="font-size:11px;color:var(--text-2);margin-top:6px">记录由系统与商家操作实时生成，不可删改；如有疑问联系掌柜小二</div>
    </div>

    <div style="padding: 6px 14px 20px" v-if="canCancel">
      <button class="btn btn-gray" style="width:100%" @click="cancel">撤销申请</button>
      <div class="minor" style="margin-top:8px">商家审核前可撤销申请</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, yuan } from '../api'
import { showToast, showConfirm } from '../ui'

const route = useRoute()
const router = useRouter()
const d = ref(null)

const canCancel = computed(() => {
  const s = String(d.value.status || '')
  return ['APPLYING', 'PENDING', 'WAIT_AUDIT', 'SUBMITTED'].includes(s)
})
// 凭证图：后端返回数组；兼容历史字符串存储（分隔符 |，dataURL 内的 ;base64, 逗号不受影响）
const proofImgs = computed(() => {
  const im = d.value?.images
  if (Array.isArray(im)) return im.filter(Boolean)
  return String(im || '').split('|').filter(Boolean)
})
function preview(src) {
  window.open(src, '_blank')
}

// 退货退款待寄回态：展示填写入口（FR-13-2，补录缺口：此前页面无入口致工单卡在待寄回）
const canReturn = computed(() => d.value?.type === 'RETURN_REFUND' && d.value?.status === 'WAIT_RETURN')
const retForm = ref({ company: '', no: '' })
async function submitLogistics() {
  if (!retForm.value.company || !retForm.value.no) {
    showToast('物流公司与运单号必填')
    return
  }
  await api.returnLogistics(d.value.afterSaleNo, retForm.value)
  showToast('已提交寄回物流')
  load()
}

function goBack() {
  if (window.history.length > 1) router.back()
  else router.replace('/orders')
}
async function load() { d.value = await api.aftersaleDetail(route.params.no) }
async function cancel() {
  const ok = await showConfirm('撤销申请', '确定撤销本次售后申请？撤销后可重新申请。')
  if (!ok) return
  await api.cancelAftersale(d.value.afterSaleNo)
  showToast('已撤销申请')
  load()
}

onMounted(load)
</script>

<style scoped>
.proof-row { display: flex; gap: 10px; flex-wrap: wrap; }
.proof-img { width: 96px; height: 96px; object-fit: cover; border-radius: 8px; border: 1px solid #eee2cf; cursor: pointer; }
</style>
