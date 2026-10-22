<template>
  <div>
    <div class="navbar-plain"><button class="back" @click="goBack">‹</button><div class="title">{{ id ? '编辑收货地址' : '新增收货地址' }}</div></div>

    <div class="card">
      <div class="field">
        <label>收货人 *</label>
        <input v-model.trim="form.receiver" placeholder="请输入收货人姓名" />
      </div>
      <div class="field">
        <label>手机号 *</label>
        <input v-model.trim="form.phone" type="tel" maxlength="11" placeholder="请输入 11 位手机号" />
      </div>
      <div class="field">
        <label>所在地区 *</label>
        <input v-model.trim="form.region" placeholder="省 / 市 / 区，如：湖南省湘潭市岳塘区" />
      </div>
      <div class="field">
        <label>详细地址 *</label>
        <input v-model.trim="form.detail" placeholder="街道、门牌号、小区、楼栋单元室号" />
      </div>
      <div style="display:flex;align-items:center;gap:8px;margin-bottom:12px">
        <input type="checkbox" id="afDefault" v-model="form.isDefault" style="width:16px;height:16px;accent-color:#a5262c" />
        <label for="afDefault" style="font-size:13px;color:var(--text);cursor:pointer">设为默认收货地址</label>
      </div>
      <button class="btn btn-main" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存' }}</button>
    </div>
    <div class="minor" style="margin-bottom:20px">手机号用于收货与到店自提核销；地址为纯文本输入，无需地图定位</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { store } from '../store'
import { showToast } from '../ui'

const route = useRoute()
const router = useRouter()
const id = route.params.id ? Number(route.params.id) : null
const saving = ref(false)
const form = ref({ receiver: '', phone: '', region: '', detail: '', isDefault: false })

function goBack() {
  if (window.history.length > 1) router.back()
  else router.replace('/addresses')
}
async function save() {
  if (!form.value.receiver) { showToast('请填写收货人姓名'); return }
  if (!/^1\d{10}$/.test(form.value.phone)) { showToast('请填写正确的 11 位手机号'); return }
  if (!form.value.region) { showToast('请填写所在地区（省 / 市 / 区）'); return }
  if (!form.value.detail) { showToast('请填写详细地址（街道、门牌号、楼栋室号）'); return }
  saving.value = true
  try {
    if (id) {
      await api.updateAddress(id, form.value)
      store.checkoutAddressId = id
    } else {
      const created = await api.addAddress(form.value)
      if (route.query.from === 'checkout') store.checkoutAddressId = created && created.id
    }
    showToast('地址已保存')
    router.back()
  } finally { saving.value = false }
}

onMounted(async () => {
  if (!id) return
  const list = await api.addresses()
  const a = list.find(x => x.id === id)
  if (a) form.value = { receiver: a.receiver, phone: a.phone, region: a.region, detail: a.detail, isDefault: !!a.isDefault }
})
</script>
