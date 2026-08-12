<template>
  <div class="login-page">
    <el-card class="login-card">
      <h1>重置密码</h1>
      <p>验证身份后设置新的登录密码。</p>
      <el-form :model="form" label-position="top" :rules="rules" ref="formRef" @submit.prevent="handleReset">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="请输入您的账号" maxlength="64" />
        </el-form-item>
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="form.oldPassword" type="password" placeholder="请输入当前密码" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" placeholder="6-64 位新密码" show-password maxlength="64" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入新密码" show-password maxlength="64" />
        </el-form-item>
        <el-button type="primary" size="large" class="login-button" :loading="loading" @click="handleReset">
          提交重置
        </el-button>
        <el-button text style="width:100%;margin-top:12px" @click="$router.push('/login')">
          返回登录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { resetPassword } from '../api/auth'

const router = useRouter()
const loading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  username: '',
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (_rule: any, value: string, callback: (e?: Error) => void) => {
  if (value !== form.newPassword) {
    callback(new Error('两次新密码输入不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 64, message: '新密码长度 6-64 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

async function handleReset() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await resetPassword({
      username: form.username,
      oldPassword: form.oldPassword,
      newPassword: form.newPassword
    })
    ElMessage.success('密码重置成功，请重新登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page { min-height: 100vh; display: grid; place-items: center; padding: 24px; background: #f5f7fb; }
.login-card { width: min(460px, 100%); border: 0; border-radius: 20px; box-shadow: 0 24px 60px rgba(15, 23, 42, 0.14); }
.login-card h1 { margin: 0 0 8px; font-size: 26px; }
.login-card p { margin: 0 0 24px; color: #64748b; line-height: 1.6; }
.login-button { width: 100%; }
</style>
