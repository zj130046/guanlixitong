<template>
  <div class="login-page">
    <el-card class="login-card">
      <h1>AI 智能客服坐席端</h1>
      <p>客服接单、处理、人工会话、超时预警和主管督办前端。</p>
      <el-form :model="form" label-position="top" @submit.prevent="handleLogin">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-button type="primary" size="large" class="login-button" :loading="loading" @click="handleLogin">
          登录
        </el-button>
        <div class="login-links">
          <span style="color:#94a3b8;font-size:13px">忘记密码请联系系统管理员</span>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({
  username: 'agent001',
  password: 'agent123'
})

async function handleLogin() {
  loading.value = true
  try {
    await auth.login(form)
    ElMessage.success('登录成功')
    router.push('/workbench')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page { min-height: 100vh; display: grid; place-items: center; padding: 24px; background: radial-gradient(circle at top left, #dbeafe, transparent 34%), #f5f7fb; }
.login-card { width: min(460px, 100%); border: 0; border-radius: 20px; box-shadow: 0 24px 60px rgba(15, 23, 42, 0.14); }
.login-card h1 { margin: 0 0 8px; font-size: 26px; }
.login-card p { margin: 0 0 24px; color: #64748b; line-height: 1.6; }
.login-button { width: 100%; }
.login-links { display: flex; justify-content: center; margin-top: 12px; }
</style>
