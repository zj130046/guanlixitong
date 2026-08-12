<template>
  <div class="dashboard">
    <!-- 欢迎横幅 -->
    <el-card class="welcome-banner">
      <div class="banner-content">
        <div class="banner-text">
          <h1>您好，{{ username }} 👋</h1>
          <p>欢迎使用 AI 智能客服工单系统，有什么可以帮您的？</p>
          <div class="banner-actions">
            <el-button type="primary" size="large" @click="goChat">
              <el-icon><ChatDotRound /></el-icon>
              开始 AI 咨询
            </el-button>
            <el-button size="large" @click="goCreateTicket">
              <el-icon><DocumentAdd /></el-icon>
              创建工单
            </el-button>
          </div>
        </div>
        <div class="banner-icon">
          <el-icon :size="100" color="#fff"><Cpu /></el-icon>
        </div>
      </div>
    </el-card>

    <!-- 数据卡片 -->
    <div class="stat-grid">
      <div class="stat-card">
        <div class="stat-icon blue">
          <el-icon><Tickets /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalTickets || 0 }}</div>
          <div class="stat-label">全部工单</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">
          <el-icon><Loading /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.pendingTickets || 0 }}</div>
          <div class="stat-label">待处理</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.completedTickets || 0 }}</div>
          <div class="stat-label">已完成</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple">
          <el-icon><ChatLineRound /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.aiReplyRate || 0 }}%</div>
          <div class="stat-label">AI 回复率</div>
        </div>
      </div>
    </div>

    <div class="content-grid">
      <!-- 最近工单 -->
      <el-card class="recent-card">
        <template #header>
          <div class="card-header">
            <span>我的最近工单</span>
            <el-button type="primary" text @click="goTickets">
              查看全部
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </template>
        <el-table :data="recentTickets" style="width: 100%" @row-click="goDetail">
          <el-table-column prop="id" label="工单号" width="90">
            <template #default="{ row }">#{{ row.id }}</template>
          </el-table-column>
          <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusColor[row.status]" size="small">
                {{ statusLabel[row.status] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="150">
            <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!recentTickets.length" description="暂无工单" :image-size="80" />
      </el-card>

      <!-- 热门 FAQ -->
      <el-card class="faq-card">
        <template #header>
          <div class="card-header">
            <span>热门问题</span>
            <el-button type="primary" text @click="goFaq">
              更多
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </template>
        <div class="hot-faq-list">
          <div
            v-for="(faq, idx) in hotFaqs"
            :key="faq.id"
            class="hot-faq-item"
            @click="goChatWithQuestion(faq.question)"
          >
            <el-tag :type="idx < 3 ? 'danger' : 'info'" size="small" class="rank">
              {{ idx + 1 }}
            </el-tag>
            <span class="faq-q">{{ faq.question }}</span>
            <el-icon class="arrow"><ArrowRight /></el-icon>
          </div>
          <el-empty v-if="!hotFaqs.length" description="暂无热门问题" :image-size="60" />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  ChatDotRound, DocumentAdd, Cpu, Tickets, Loading, CircleCheck,
  ChatLineRound, ArrowRight
} from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { listUserTickets, statusColor, statusLabel } from '../api/ticket'
import { getStatisticsOverview } from '../api/stats'
import { listFaqEntries } from '../api/faq'
import type { Ticket } from '../api/ticket'
import type { FaqEntry } from '../api/faq'

const router = useRouter()
const auth = useAuthStore()
const username = computed(() => auth.username || '用户')

const stats = ref({
  totalTickets: 0,
  pendingTickets: 0,
  completedTickets: 0,
  aiReplyRate: 0
})

const recentTickets = ref<Ticket[]>([])
const hotFaqs = ref<FaqEntry[]>([])

onMounted(() => {
  loadStats()
  loadRecentTickets()
  loadHotFaqs()
})

/** 从统计 API 获取全局统计数据 */
async function loadStats() {
  try {
    const data = await getStatisticsOverview() as any
    stats.value.totalTickets = data.totalTickets || 0
    stats.value.pendingTickets = data.pendingTickets || 0
    stats.value.completedTickets = data.completedTickets || 0
    stats.value.aiReplyRate = data.aiReplyRate || 0
  } catch (e) { /* ignore */ }
}

async function loadRecentTickets() {
  try {
    const res = await listUserTickets({ page: 1, size: 5 }) as any
    recentTickets.value = res.records || res || []
  } catch (e) { /* ignore */ }
}

async function loadHotFaqs() {
  try {
    const res = await listFaqEntries() as any
    hotFaqs.value = (res.records || res || []).slice(0, 6)
  } catch (e) {
    // ignore
  }
}

function goChat() { router.push('/chat') }
function goCreateTicket() { router.push('/tickets/create') }
function goTickets() { router.push('/tickets') }
function goFaq() { router.push('/faq') }
function goDetail(row: Ticket) { router.push(`/tickets/${row.id}`) }
function goChatWithQuestion(q: string) {
  router.push({ path: '/chat', query: { q } })
}

function formatTime(time: string) {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getMonth() + 1}-${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped>
.dashboard {
  max-width: 1200px;
  margin: 0 auto;
}

.welcome-banner {
  margin-bottom: 20px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: #fff;
}
.welcome-banner :deep(.el-card__body) { padding: 0; }

.banner-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 32px 36px;
}

.banner-text h1 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: #fff;
}
.banner-text p {
  margin: 0 0 20px 0;
  color: rgba(255,255,255,0.85);
  font-size: 14px;
}
.banner-actions { display: flex; gap: 12px; }

.banner-icon { opacity: 0.9; }

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-size: 24px;
  color: #fff;
}
.stat-icon.blue { background: linear-gradient(135deg, #60a5fa, #3b82f6); }
.stat-icon.orange { background: linear-gradient(135deg, #fbbf24, #f59e0b); }
.stat-icon.green { background: linear-gradient(135deg, #34d399, #10b981); }
.stat-icon.purple { background: linear-gradient(135deg, #a78bfa, #8b5cf6); }

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
}
.stat-label {
  font-size: 13px;
  color: #64748b;
  margin-top: 2px;
}

.content-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}

.recent-card, .faq-card { border-radius: 12px; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #1e293b;
}

.hot-faq-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.hot-faq-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}
.hot-faq-item:hover {
  background: #f1f5f9;
}

.rank { flex-shrink: 0; }
.faq-q {
  flex: 1;
  font-size: 14px;
  color: #334155;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.arrow { color: #94a3b8; font-size: 12px; }

@media (max-width: 900px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .content-grid { grid-template-columns: 1fr; }
}
</style>
