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
