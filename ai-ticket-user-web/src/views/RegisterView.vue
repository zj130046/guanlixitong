<template>
  <div class="login-page">
    <el-card class="login-card">
      <h1>用户注册</h1>
      <p>创建新账号，享受 AI 智能客服与工单追踪服务。</p>
      <el-form :model="form" label-position="top" :rules="rules" ref="formRef" @submit.prevent="handleRegister">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="3-64 位字符" maxlength="64" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="6-64 位密码" show-password maxlength="64" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" show-password maxlength="64" />
        </el-form-item>
        <el-form-item label="手机号（选填）">
          <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="32" />
        </el-form-item>
        <el-form-item label="邮箱（选填）">
          <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="128" />
        </el-form-item>
        <el-button type="primary" size="large" class="login-button" :loading="loading" @click="handleRegister">
          注册
        </el-button>
        <el-button text style="width:100%;margin-top:12px" @click="$router.push('/login')">
          已有账号？返回登录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { register as requestRegister } from '../api/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  phone: '',
  email: ''
})

const validateConfirm = (_rule: any, value: string, callback: (e?: Error) => void) => {
  if (value !== form.password) {
    callback(new Error('两次密码输入不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 64, message: '账号长度 3-64 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度 6-64 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const response = await requestRegister({
      username: form.username,
      password: form.password,
      phone: form.phone || undefined,
      email: form.email || undefined
    })
    // 注册成功后通过 store 统一登录（保证状态一致性）
    await auth.login({ username: form.username, password: form.password })
    ElMessage.success('注册成功')
    router.push('/dashboard')
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
</style>
