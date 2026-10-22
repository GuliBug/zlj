<template>
  <div>
    <div class="login-hero">
      <div class="seal">正隆記</div>
      <div class="since">· 始于一九一六 ·</div>
      <div class="motto">以正立本 · 以究待客</div>
    </div>

    <div class="card" style="margin-top:-24px">
      <div class="form-item">
        <label>手机号</label>
        <input v-model.trim="phone" type="tel" maxlength="11" placeholder="请输入11位手机号" />
      </div>
      <div class="form-item">
        <label>验证码</label>
        <input v-model.trim="smsCode" type="tel" maxlength="6" placeholder="请输入验证码" />
      </div>
      <div class="muted" style="padding:10px 0 4px;font-size:12px">演示环境，验证码固定为 123456；新手机号将自动注册</div>
      <button class="btn" style="width:100%;margin-top:12px" :disabled="loading" @click="doLogin">
        {{ loading ? '登录中…' : '登录 / 注册' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, setToken } from '../api'
import { showToast } from '../ui'
import { refreshCartCount } from '../store'

const route = useRoute()
const router = useRouter()
const phone = ref('')
const smsCode = ref('')
const loading = ref(false)

async function doLogin() {
  if (!/^1\d{10}$/.test(phone.value)) { showToast('请输入正确的11位手机号'); return }
  if (!smsCode.value) { showToast('请输入验证码'); return }
  loading.value = true
  try {
    const d = await api.login(phone.value, smsCode.value)
    setToken(d.token)
    refreshCartCount()
    showToast('欢迎回来，正隆记顾客')
    router.replace(route.query.redirect || '/member')
  } finally { loading.value = false }
}
</script>

<style scoped>
.login-hero {
  background: linear-gradient(160deg, #8E181C, #B01F24 75%, #C43A34);
  text-align: center; padding: 60px 20px 50px; color: #F7ECD8;
  border-bottom-left-radius: 26px; border-bottom-right-radius: 26px;
}
.seal {
  width: 84px; height: 84px; margin: 0 auto 14px;
  border: 2px solid #E8D5B0; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 26px; font-weight: 700; letter-spacing: 3px; writing-mode: vertical-lr;
  background: rgba(0,0,0,.12);
}
.since { font-size: 15px; letter-spacing: 3px; }
.motto { font-size: 12px; color: #E8D5B0; margin-top: 8px; letter-spacing: 2px; }
</style>
