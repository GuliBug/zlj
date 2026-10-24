<template>
  <div>
    <div class="navbar-plain"><button class="back" @click="goBack">‹</button><div class="title">申请售后</div></div>

    <!-- 售后说明 -->
    <div class="card">
      <div class="card-title">售后说明</div>
      <div style="font-size:12px;color:var(--text-2);line-height:1.7">
        已签收 7 天内且商品未拆封可申请退货退款；<br>
        已拆封商品仅支持质量问题协商退款；<br>
        门店自提订单请到店由掌柜协助处理。
      </div>
    </div>

    <div class="card">
      <div class="field">
        <label>售后类型</label>
        <select v-model="form.type">
          <option value="RETURN_REFUND">退货退款（未拆封）</option>
          <option value="REFUND_ONLY">仅退款（质量问题协商）</option>
        </select>
      </div>
      <div class="field">
        <label>售后原因</label>
        <select v-model="form.reason">
          <option value="" disabled>请选择原因</option>
          <option v-for="r in reasons" :key="r" :value="r">{{ r }}</option>
        </select>
      </div>
      <div class="field">
        <label>问题描述（选填）</label>
        <textarea v-model.trim="form.description" placeholder="请描述问题，质量问题需要上传凭证"></textarea>
      </div>
      <div class="field">
        <label>凭证图片（质量问题必填，1-3 张）</label>
        <div style="display:flex;gap:10px">
          <div v-for="i in 3" :key="i" class="proof-slot" :class="{ on: proofs[i - 1] }" @click="onSlotClick(i)">
            <img v-if="proofs[i - 1]" :src="proofs[i - 1]" class="proof-img">
            <span v-else>＋</span>
            <span v-if="proofs[i - 1]" class="proof-del">×</span>
          </div>
          <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="onFileChange">
        </div>
        <div class="muted" style="font-size:11px;margin-top:6px">点击空位选择本地图片，自动压缩后随申请提交；点击已传图片可删除</div>
      </div>
      <button class="btn btn-main" :disabled="submitting" @click="submit">
        {{ submitting ? '提交中…' : '提交申请' }}
      </button>
    </div>
    <div class="minor" style="margin-bottom:20px">每个订单每个商品限申请 3 次；提交后可在「售后详情」查看每一步流转记录</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { showToast } from '../ui'
import { compressImage } from '../image'

const route = useRoute()
const router = useRouter()
const orderNo = route.params.orderNo
const reasons = ref([])
const proofs = ref([])
const fileInput = ref(null)
const submitting = ref(false)
const form = ref({ type: 'RETURN_REFUND', reason: '', description: '' })

function goBack() {
  if (window.history.length > 1) router.back()
  else router.replace('/orders')
}
// 点已传槽位删除，点空槽位唤起文件选择
function onSlotClick(i) {
  if (proofs.value[i - 1]) proofs.value.splice(i - 1, 1)
  else fileInput.value?.click()
}
async function onFileChange(e) {
  const file = e.target.files?.[0]
  e.target.value = ''
  if (!file) return
  const dataUrl = await compressImage(file)
  if (!dataUrl) { showToast('图片读取失败，请换一张'); return }
  if (proofs.value.length >= 3) { showToast('最多上传 3 张凭证'); return }
  proofs.value.push(dataUrl)
}
async function submit() {
  if (!form.value.reason) { showToast('请选择售后原因'); return }
  if (/质量问题/.test(form.value.reason) && !proofs.value.length) {
    showToast('质量问题需上传凭证图片')
    return
  }
  submitting.value = true
  try {
    const d = await api.createAftersale({
      orderNo,
      type: form.value.type,
      reason: form.value.reason,
      description: form.value.description,
      images: proofs.value
    })
    showToast('售后申请已提交（工单 ' + d.afterSaleNo + '）')
    router.replace('/aftersale/' + d.afterSaleNo)
  } finally { submitting.value = false }
}

onMounted(async () => { reasons.value = (await api.aftersaleReasons()) || [] })
</script>

<style scoped>
.proof-slot {
  position: relative;
  width: 64px; height: 64px; border: 1px dashed #cfc4b0; border-radius: 8px;
  display: flex; align-items: center; justify-content: center; color: #cfc4b0; font-size: 24px; cursor: pointer;
  overflow: hidden;
}
.proof-slot.on { border-color: var(--brand); }
.proof-img { width: 100%; height: 100%; object-fit: cover; }
.proof-del {
  position: absolute; top: 0; right: 0; width: 16px; height: 16px; line-height: 14px;
  background: rgba(0,0,0,.55); color: #fff; font-size: 12px; text-align: center; border-radius: 0 0 0 8px;
}
</style>
