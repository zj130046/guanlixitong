import { createRouter, createWebHistory } from 'vue-router'
import BasicLayout from '../layouts/BasicLayout.vue'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
  { path: '/login', component: () => import('../views/LoginView.vue'), meta: { title: '登录', public: true } },
    {
      path: '/',
      component: BasicLayout,
      redirect: '/workbench',
      children: [
      { path: 'workbench', component: () => import('../views/WorkbenchView.vue'), meta: { title: '坐席工作台', menu: true } },
      { path: 'tickets/pool', component: () => import('../views/TicketPoolView.vue'), meta: { title: '工单池', menu: true } },
      { path: 'tickets/mine', component: () => import('../views/MyTicketsView.vue'), meta: { title: '我的工单', menu: true } },
      { path: 'tickets/:id', component: () => import('../views/TicketDetailView.vue'), meta: { title: '工单详情', menu: false } },
      { path: 'chat/:conversationId', component: () => import('../views/HumanChatView.vue'), meta: { title: '人工会话', menu: false } },
      { path: 'warnings', component: () => import('../views/TimeoutWarningView.vue'), meta: { title: '超时预警', menu: true } },
      { path: 'supervision', component: () => import('../views/SupervisionView.vue'), meta: { title: '主管督办', menu: true } },
      { path: 'statistics', component: () => import('../views/AgentStatsView.vue'), meta: { title: '坐席统计', menu: true } },
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
    return '/workbench'
  }
  return true
})

export default router
