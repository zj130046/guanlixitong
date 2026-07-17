<template>
  <el-container class="layout">
    <el-aside width="236px" class="layout__aside">
      <div class="layout__brand">
        <div class="layout__logo">AI</div>
        <div>
          <strong>AI 智能客服管理后台</strong>
          <span>工单处理系统</span>
        </div>
      </div>
      <el-menu :default-active="route.path" router class="layout__menu">
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout__header">
        <div>
          <strong>{{ currentTitle }}</strong>
          <span class="layout__subtitle">7×24 小时智能接待 · 工单全流程追踪</span>
        </div>
        <div class="layout__user">
          <span>{{ auth.username || '未登录' }}</span>
          <el-button type="primary" plain @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="layout__main">
        <RouterView />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const menuItems = computed(() =>
  router
    .getRoutes()
    .filter((item) => item.meta.menu)
    .map((item) => ({ path: item.path, title: String(item.meta.title || item.name || item.path) }))
)

const currentTitle = computed(() => String(route.meta.title || '工作台'))

async function handleLogout() {
  await auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout { min-height: 100vh; }
.layout__aside { background: #0f172a; color: #fff; }
.layout__brand { display: flex; gap: 12px; align-items: center; padding: 20px; }
.layout__brand span { display: block; margin-top: 4px; color: #94a3b8; font-size: 12px; }
.layout__logo { display: grid; place-items: center; width: 42px; height: 42px; border-radius: 12px; background: #2563eb; font-weight: 800; }
.layout__menu { border-right: 0; background: transparent; }
.layout__menu :deep(.el-menu-item) { color: #cbd5e1; }
.layout__menu :deep(.el-menu-item.is-active) { color: #fff; background: rgba(37, 99, 235, 0.36); }
.layout__header { display: flex; align-items: center; justify-content: space-between; background: #fff; border-bottom: 1px solid #e2e8f0; }
.layout__subtitle { margin-left: 12px; color: #64748b; font-size: 13px; }
.layout__user { display: flex; align-items: center; gap: 12px; }
.layout__main { padding: 24px; }
</style>
