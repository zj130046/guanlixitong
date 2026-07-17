<template>
  <div class="detail-page">
    <div v-loading="loading" class="detail-content">
      <!-- 错误状态 -->
      <el-card v-if="loadError" class="error-card">
        <el-result icon="error" title="加载失败" sub-title="工单详情加载失败，请重试">
          <template #extra>
            <el-button type="primary" @click="loadDetail">重新加载</el-button>
            <el-button @click="goBack">返回</el-button>
          </template>
        </el-result>
      </el-card>

      <template v-else>
      <!-- 顶部信息条 -->
      <div class="top-bar">
        <el-page-header @back="goBack" content="工单详情">
          <template #content>
            <div class="top-title">
              <span class="ticket-id">#{{ ticket?.id }}</span>
              <el-tag :type="statusColor[ticket?.status || '']" size="large" effect="dark">
                {{ statusLabel[ticket?.status || ''] || ticket?.status }}
              </el-tag>
            </div>
          </template>
          <template #extra>
            <div class="top-actions">
              <el-button @click="handleRefresh">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
              <el-button
                v-if="ticket?.status === 'COMPLETED'"
                type="primary"
                @click="showSatisfaction = true"
              >
                <el-icon><Star /></el-icon>
                评价
              </el-button>
            </div>
          </template>
        </el-page-header>
      </div>

      <div class="detail-grid">
        <!-- 左侧：工单信息 -->
        <div class="left-col">
          <el-card class="info-card">
            <template #header>
              <div class="card-title">
                <el-icon><Tickets /></el-icon>
                工单信息
              </div>
            </template>

            <h2 class="ticket-title">{{ ticket?.title }}</h2>

            <el-descriptions :column="2" border size="default" class="desc">
              <el-descriptions-item label="工单号">#{{ ticket?.id }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="statusColor[ticket?.status || '']">
                  {{ statusLabel[ticket?.status || ''] }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="分类">{{ ticket?.category || '未分类' }}</el-descriptions-item>
              <el-descriptions-item label="优先级">
                <el-tag :type="priorityColor[ticket?.priority || '']">
                  {{ priorityLabel[ticket?.priority || ''] }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="处理人">{{ ticket?.assignee || '待分配' }}</el-descriptions-item>
              <el-descriptions-item label="来源">
                {{ ticket?.source === 'AI_CHAT' ? 'AI 对话转人工' : '用户提交' }}
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ formatTime(ticket?.createdAt) }}</el-descriptions-item>
              <el-descriptions-item label="更新时间">{{ formatTime(ticket?.updatedAt) }}</el-descriptions-item>
              <el-descriptions-item label="截止时间">
                <span :class="{ 'timeout-warning': isTimeout }">
                  {{ formatTime(ticket?.timeoutAt) }}
                  <el-tag v-if="isTimeout" type="danger" size="small" style="margin-left: 8px">已超时</el-tag>
                </span>
              </el-descriptions-item>
            </el-descriptions>

            <div class="desc-section">
              <div class="section-title">问题描述</div>
              <div class="section-content">{{ ticket?.description }}</div>
            </div>
          </el-card>

          <!-- 流转时间线 -->
          <el-card class="timeline-card">
            <template #header>
              <div class="card-title">
                <el-icon><Clock /></el-icon>
                处理进度
              </div>
            </template>
            <el-timeline>
              <el-timeline-item
                v-for="(event, idx) in reversedEvents"
                :key="event.id"
                :timestamp="formatTime(event.createdAt)"
                :type="timelineType(event, idx)"
                :hollow="idx === 0"
              >
                <div class="event-item">
                  <div class="event-title">
                    <strong>{{ eventLabel(event.eventType) }}</strong>
                    <span class="event-operator">
                      {{ operatorLabel(event.operatorType) }}
                    </span>
                  </div>
                  <div class="event-remark" v-if="event.remark">{{ event.remark }}</div>
                  <div class="event-status" v-if="event.toStatus">
                    状态：{{ statusLabel[event.toStatus] || event.toStatus }}
                  </div>
                </div>
              </el-timeline-item>
              <el-timeline-item v-if="!ticket?.events?.length" type="info">
                暂无处理记录
              </el-timeline-item>
            </el-timeline>
          </el-card>
        </div>

        <!-- 右侧：快捷信息 -->
        <div class="right-col">
          <el-card class="side-card">
            <template #header>
              <div class="card-title">
                <el-icon><InfoFilled /></el-icon>
                进度概览
              </div>
            </template>
            <el-steps :active="currentStep" direction="vertical" finish-status="success">
              <el-step title="提交工单" description="用户提交问题" />
              <el-step title="分配受理" description="系统分配客服" />
              <el-step title="处理中" description="客服处理问题" />
              <el-step title="工单完结" description="处理完成" />
            </el-steps>
          </el-card>

          <el-card class="side-card">
            <template #header>
              <div class="card-title">
                <el-icon><User /></el-icon>
                提交人信息
              </div>
            </template>
            <div class="user-info">
              <el-avatar :size="48">{{ (ticket?.requesterName || '用户').charAt(0) }}</el-avatar>
              <div class="user-detail">
                <div class="user-name">{{ ticket?.requesterName || '用户' + ticket?.userId }}</div>
                <div class="user-id">ID: {{ ticket?.userId }}</div>
              </div>
            </div>
          </el-card>
        </div>
      </div>
      </template>
    </div>

    <!-- 满意度评价弹窗 -->
    <el-dialog v-model="showSatisfaction" title="服务满意度评价" width="500px">
      <div class="satisfaction-form">
        <div class="rate-section">
          <div class="rate-label">服务评分</div>
          <el-rate v-model="satisfactionForm.score" :max="5" size="large" show-text />
        </div>
        <div class="rate-section">
          <div class="rate-label">评价内容</div>
          <el-input
            v-model="satisfactionForm.comment"
            type="textarea"
            :rows="4"
            placeholder="请输入您的评价和建议（选填）"
            maxlength="200"
            show-word-limit
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="showSatisfaction = false">取消</el-button>
        <el-button type="primary" :loading="submittingSatisfaction" @click="submitSatisfaction">
          提交评价
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Refresh, Star, Tickets, Clock, InfoFilled, User
} from '@element-plus/icons-vue'
import {
  getTicket, submitSatisfaction as submitSatisfactionApi,
  statusColor, statusLabel, priorityColor, priorityLabel
} from '../api/ticket'
import type { Ticket, TicketEvent } from '../api/ticket'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const loadError = ref(false)
const ticket = ref<Ticket | null>(null)
const showSatisfaction = ref(false)
const submittingSatisfaction = ref(false)

const satisfactionForm = reactive({
  score: 5,
  comment: ''
})

const reversedEvents = computed(() => {
  return [...(ticket.value?.events || [])].reverse()
})

const isTimeout = computed(() => {
  if (!ticket.value?.timeoutAt) return false
  return new Date(ticket.value.timeoutAt) < new Date()
    && ticket.value.status !== 'COMPLETED'
    && ticket.value.status !== 'REJECTED'
})

const currentStep = computed(() => {
  const status = ticket.value?.status
  if (status === 'COMPLETED' || status === 'ARCHIVED') return 4
  if (status === 'PROCESSING' || status === 'FOLLOWING') return 3
  if (status === 'ACCEPTED') return 2
  if (status === 'ASSIGNED') return 1
  return 0
})

onMounted(loadDetail)

async function loadDetail() {
  const id = route.params.id as string
  if (!id) {
    loadError.value = true
    return
  }
  loading.value = true
  loadError.value = false
  try {
    ticket.value = await getTicket(id)
  } catch (e) {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

function handleRefresh() {
  loadDetail()
}

function goBack() {
  router.push('/tickets')
}

async function submitSatisfaction() {
  if (!ticket.value) return
  submittingSatisfaction.value = true
  try {
    await submitSatisfactionApi({
      ticketId: ticket.value.id,
      score: satisfactionForm.score,
      comment: satisfactionForm.comment
    }) as any
    ElMessage.success('评价提交成功，感谢您的反馈！')
    showSatisfaction.value = false
    loadDetail()
  } catch (e: any) {
    ElMessage.error(e.message || '提交失败')
  } finally {
    submittingSatisfaction.value = false
  }
}

function eventLabel(type: string) {
  const map: Record<string, string> = {
    CREATE: '工单创建',
    ACCEPT: '客服接单',
    PROCESS: '开始处理',
    FOLLOW: '持续跟进',
    COMPLETE: '工单完结',
    REJECT: '工单驳回',
    ADMIN_UPDATE: '管理员更新',
    TRANSFER: '工单转派'
  }
  return map[type] || type
}

function operatorLabel(type: string) {
  const map: Record<string, string> = {
    USER: '用户',
    AGENT: '客服',
    ADMIN: '管理员',
    SYSTEM: '系统'
  }
  return map[type] || type
}

function timelineType(event: TicketEvent, idx: number) {
  if (idx === 0) return 'primary'
  if (event.eventType === 'COMPLETE') return 'success'
  if (event.eventType === 'REJECT') return 'danger'
  return ''
}

function formatTime(time?: string) {
  if (!time) return '-'
  const d = new Date(time)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped>
.detail-page {
  min-height: 600px;
}

.error-card {
  border-radius: 12px;
  margin-bottom: 16px;
}

.top-bar {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

.top-title {
  display: flex;
  align-items: center;
  gap: 12px;
}
.ticket-id {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}
.top-actions { display: flex; gap: 8px; }

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 16px;
}

.info-card, .timeline-card, .side-card {
  border-radius: 12px;
  margin-bottom: 16px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #1e293b;
}

.ticket-title {
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 16px 0;
}

.desc { margin-top: 8px; }

.desc-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #f1f5f9;
}
.section-title {
  font-weight: 600;
  color: #334155;
  margin-bottom: 8px;
}
.section-content {
  color: #475569;
  line-height: 1.8;
  white-space: pre-wrap;
  background: #f8fafc;
  padding: 12px 16px;
  border-radius: 8px;
}

.timeout-warning { color: #dc2626; }

.event-item { padding: 4px 0; }
.event-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.event-operator {
  font-size: 12px;
  color: #64748b;
}
.event-remark {
  color: #475569;
  font-size: 13px;
  margin-top: 4px;
}
.event-status {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-name { font-weight: 500; color: #1e293b; }
.user-id { font-size: 12px; color: #94a3b8; }

.satisfaction-form { padding: 10px 0; }
.rate-section { margin-bottom: 20px; }
.rate-label {
  font-weight: 500;
  color: #334155;
  margin-bottom: 12px;
}

@media (max-width: 900px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
