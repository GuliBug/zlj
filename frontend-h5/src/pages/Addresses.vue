<template>
  <div>
    <div class="navbar-plain"><button class="back" @click="goBack">‹</button><div class="title">收货地址</div></div>

    <div class="minor" style="margin-top:10px">{{ fromCheckout ? '点击地址卡片即可选用为本次订单的收货地址' : '管理你的收货地址，支持新增 / 编辑 / 删除' }}</div>

    <div v-if="rows.length">
      <div v-for="a in rows" :key="a.id" class="card addr-item" @click="onPick(a)">
        <div style="display:flex;justify-content:space-between;align-items:center">
          <b style="font-family:var(--serif)">{{ a.receiver }} {{ maskPhone(a.phone) }}</b>
          <span v-if="a.isDefault" style="font-size:10px;background:var(--gold);color:#5d3a10;border-radius:8px;padding:2px 8px;font-weight:700">默认</span>
        </div>
        <div style="font-size:12px;color:var(--text-2);margin-top:6px;line-height:1.6">{{ a.region }} {{ a.detail }}</div>
        <div style="display:flex;gap:8px;margin-top:10px" @click.stop>
          <template v-if="!fromCheckout">
            <button class="af-btn" @click="$router.push('/address-edit/' + a.id)">编辑</button>
            <button class="af-btn danger" @click="del(a)">删除</button>
            <button v-if="!a.isDefault" class="af-btn" @click="setDefault(a)">设为默认</button>
          </template>
          <span v-else class="muted" style="font-size:12px">点击选用此地址</span>
        </div>
      </div>
    </div>
    <div class="result" v-else-if="loaded" style="padding-top:60px"><p>暂无收货地址</p></div>

    <div style="padding:6px 12px 12px">
      <button class="btn btn-main" @click="addNew">＋ 新增收货地址</button>
    </div>
    <div class="minor">地址为纯文本输入，无需地图定位；请填写省/市/区与详细门牌信息</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { store } from '../store'
import { showToast, showConfirm } from '../ui'

const route = useRoute()
const router = useRouter()
const rows = ref([])
const loaded = ref(false)
const fromCheckout = computed(() => route.query.from === 'checkout')

function maskPhone(p) { return p ? p.replace(/^(\d{3})\d{4}(\d{4})$/, '$1****$2') : '' }
function goBack() {
  if (fromCheckout.value) router.back()
  else if (window.history.length > 1) router.back()
  else router.replace('/member')
}
function addNew() {
  router.push('/address-edit' + (fromCheckout.value ? '?from=checkout' : ''))
}
async function load() {
  rows.value = await api.addresses()
  loaded.value = true
}
function onPick(a) {
  if (fromCheckout.value) {
    store.checkoutAddressId = a.id
    router.back()
  }
}
async function setDefault(a) {
  await api.setDefaultAddress(a.id)
  showToast('已设为默认收货地址')
  load()
}
async function del(a) {
  const ok = await showConfirm('删除地址', `确定删除「${a.receiver}」的收货地址？`)
  if (!ok) return
  await api.deleteAddress(a.id)
  showToast('地址已删除')
  load()
}

onMounted(load)
</script>
