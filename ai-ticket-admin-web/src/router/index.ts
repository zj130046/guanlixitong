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
      redirect: '/dashboard',
      children: [
      { path: 'dashboard', component: () => import('../views/DashboardView.vue'), meta: { title: '首页看板', menu: true } },
      { path: 'users', component: () => import('../views/UserManageView.vue'), meta: { title: '用户管理', menu: true } },
      { path: 'agents', component: () => import('../views/AgentManageView.vue'), meta: { title: '客服管理', menu: true } },
      { path: 'agent-groups', component: () => import('../views/AgentGroupManageView.vue'), meta: { title: '客服分组', menu: true } },
      { path: 'tickets', component: () => import('../views/TicketManageView.vue'), meta: { title: '工单管理', menu: true } },
      { path: 'tickets/archive', component: () => import('../views/TicketArchiveView.vue'), meta: { title: '工单归档', menu: true } },
      { path: 'tickets/:id', component: () => import('../views/TicketDetailView.vue'), meta: { title: '工单详情', menu: false } },
      { path: 'faq/categories', component: () => import('../views/FaqCategoryView.vue'), meta: { title: 'FAQ 分类', menu: true } },
      { path: 'faq/entries', component: () => import('../views/FaqEntryView.vue'), meta: { title: 'FAQ 问答', menu: true } },
      { path: 'faq/similarity', component: () => import('../views/SimilarityConfigView.vue'), meta: { title: '语义匹配配置', menu: true } },
      { path: 'reports/monthly', component: () => import('../views/MonthlyReportView.vue'), meta: { title: '月度报表', menu: true } },
      { path: 'statistics/satisfaction', component: () => import('../views/SatisfactionStatsView.vue'), meta: { title: '满意度统计', menu: true } },
      { path: 'settings', component: () => import('../views/SystemSettingsView.vue'), meta: { title: '系统配置', menu: true } },
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
