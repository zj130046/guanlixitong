<template>
  <div class="login-page">
    <el-card class="login-card">
      <h1>重置密码</h1>
      <p>通过账号信息重置用户端登录密码。</p>
      <el-form label-position="top">
        <el-form-item label="账号">
          <el-input placeholder="请输入需要重置的账号" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
        <el-button type="primary">提交重置申请</el-button>
        <el-button text @click="$router.push('/login')">返回登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.login-page { min-height: 100vh; display: grid; place-items: center; padding: 24px; background: #f5f7fb; }
.login-card { width: min(460px, 100%); border: 0; border-radius: 20px; }
</style>
