<template>
  <div class="login-wrap">
    <div class="login-card">
      <div class="login-logo">
        <span class="seal">記</span>
        <h1>正隆記 · 门店运营后台</h1>
      </div>
      <el-form :model="form" @keyup.enter="handleLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="账号" size="large">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" show-password placeholder="密码" size="large">
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="handleLogin">登 录</el-button>
        </el-form-item>
      </el-form>
      <div class="login-tip">账号提示：admin / admin123</div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/dashboard'
import useUserStore from '@/store/user'

const router = useRouter()
const route = useRoute()
const user = useUserStore()
const loading = ref(false)
const form = reactive({ username: 'admin', password: '' })

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号与密码')
    return
  }
  loading.value = true
  try {
    const res = await login({ username: form.username, password: form.password })
    user.setUser(res.data.token, res.data.username, res.data.nickname)
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #241d18 0%, #3d2420 55%, #8f1d22 130%);
}
.login-card {
  width: 380px;
  background: #fffdf8;
  border-radius: 12px;
  padding: 36px 32px 24px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.4);
  border-top: 4px solid #8f1d22;
}
.login-logo {
  text-align: center;
  margin-bottom: 24px;
}
.login-logo .seal {
  display: inline-flex;
  width: 52px;
  height: 52px;
  align-items: center;
  justify-content: center;
  background: #8f1d22;
  color: #c9a063;
  font-size: 30px;
  font-weight: bold;
  border-radius: 8px;
  margin-bottom: 10px;
}
.login-logo h1 {
  font-size: 18px;
  color: #8f1d22;
  margin: 0;
  letter-spacing: 2px;
  font-family: KaiTi, 'STKaiti', serif;
}
.login-tip {
  text-align: center;
  color: #8a8074;
  font-size: 12px;
}
</style>
