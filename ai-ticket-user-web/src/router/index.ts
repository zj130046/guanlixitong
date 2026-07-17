import { createRouter, createWebHistory } from 'vue-router'
import BasicLayout from '../layouts/BasicLayout.vue'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
  { path: '/login', component: () => import('../views/LoginView.vue'), meta: { title: '登录', public: true } },
  { path: '/reset-password', component: () => import('../views/ResetPasswordView.vue'), meta: { title: '重置密码', public: true } },
    {
      path: '/',
      component: BasicLayout,
      redirect: '/dashboard',
      children: [
      { path: 'dashboard', component: () => import('../views/DashboardView.vue'), meta: { title: '用户首页', menu: true } },
      { path: 'tickets/create', component: () => import('../views/TicketCreateView.vue'), meta: { title: '创建工单', menu: true } },
      { path: 'tickets', component: () => import('../views/TicketListView.vue'), meta: { title: '我的工单', menu: true } },
      { path: 'tickets/:id', component: () => import('../views/TicketDetailView.vue'), meta: { title: '工单详情', menu: false } },
      { path: 'chat', component: () => import('../views/AiChatView.vue'), meta: { title: 'AI 智能咨询', menu: true } },
      { path: 'conversations', component: () => import('../views/ConversationHistoryView.vue'), meta: { title: '历史会话', menu: true } },
      { path: 'faq', component: () => import('../views/FaqView.vue'), meta: { title: 'FAQ 知识库', menu: true } },
      { path: 'satisfaction/:ticketId', component: () => import('../views/SatisfactionView.vue'), meta: { title: '满意度评价', menu: false } },
        { path: ':pathMatch(.*)*', component: () => import('../views/NotFoundView.vue'), meta: { title: '页面不存在', menu: false } }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isLoggedIn) {
    return '/login'
  }
  if (to.path === '/login' && auth.isLoggedIn) {
    return '/dashboard'
  }
  return true
})

export default router
