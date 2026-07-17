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
        <!-- 顶部 -->
        <el-card class="top-bar">
          <div class="top-left">
            <el-page-header @back="goBack" content="工单详情">
              <template #content>
                <div class="top-title">
                  <span class="ticket-id">#{{ ticket?.id }}</span>
                  <el-tag :type="statusColor[ticket?.status || '']" size="large" effect="dark">
                    {{ statusLabel[ticket?.status || ''] }}
                  </el-tag>
                  <el-tag :type="priorityColor[ticket?.priority || '']" effect="plain" size="large">
                    {{ priorityLabel[ticket?.priority || ''] }}优先级
                  </el-tag>
                </div>
              </template>
            </el-page-header>
          </div>
          <div class="top-actions">
            <el-button @click="handleAction('priority')">
              <el-icon><Warning /></el-icon>
              调整优先级
            </el-button>
            <el-button @click="handleAction('assign')">
              <el-icon><User /></el-icon>
              改派工单
            </el-button>
            <el-button type="danger" v-if="ticket?.status !== 'ARCHIVED'" @click="handleAction('archive')">
              <el-icon><FolderChecked /></el-icon>
              归档工单
            </el-button>
          </div>
        </el-card>

        <div class="detail-grid">
          <!-- 左侧主内容 -->
          <div class="left-col">
            <!-- 工单信息 -->
            <el-card class="info-card">
              <template #header>
                <div class="card-title"><el-icon><Tickets /></el-icon>工单信息</div>
              </template>
              <h2 class="ticket-title">{{ ticket?.title }}</h2>
              <el-descriptions :column="2" border size="default">
                <el-descriptions-item label="工单号">#{{ ticket?.id }}</el-descriptions-item>
                <el-descriptions-item label="状态">
                  <el-tag :type="statusColor[ticket?.status || '']">{{ statusLabel[ticket?.status || ''] }}</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="分类">{{ ticket?.category || '未分类' }}</el-descriptions-item>
                <el-descriptions-item label="部门">{{ ticket?.department || '客服中心' }}</el-descriptions-item>
                <el-descriptions-item label="提交人">{{ ticket?.requesterName || '-' }}</el-descriptions-item>
                <el-descriptions-item label="来源">
                  {{ ticket?.source === 'AI_CHAT' ? 'AI 对话转人工' : '用户提交' }}
                </el-descriptions-item>
                <el-descriptions-item label="处理人">{{ ticket?.assignee || '待分配' }}</el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ formatTime(ticket?.createdAt) }}</el-descriptions-item>
                <el-descriptions-item label="截止时间">
                  <span :class="{ 'timeout-text': isTimeout }">
                    {{ formatTime(ticket?.timeoutAt) }}
                    <el-tag v-if="isTimeout" type="danger" size="small" style="margin-left: 6px">已超时</el-tag>
                  </span>
                </el-descriptions-item>
                <el-descriptions-item label="完结时间">{{ formatTime(ticket?.completedAt) }}</el-descriptions-item>
              </el-descriptions>
              <div class="desc-section">
                <div class="section-title">问题描述</div>
                <div class="desc-content">{{ ticket?.description }}</div>
              </div>
            </el-card>

            <!-- 处理进度时间线 -->
            <el-card class="timeline-card">
              <template #header>
                <div class="card-title"><el-icon><Clock /></el-icon>处理进度</div>
              </template>
              <el-timeline>
                <el-timeline-item
                  v-for="(event, idx) in reversedEvents"
                  :key="event.id"
                  :timestamp="formatTime(event.createdAt)"
                  :type="timelineType(event, idx)"
                  :hollow="idx === 0"
                  placement="top"
                >
                  <div class="event-item">
                    <div class="event-title">
                      <strong>{{ eventLabel(event.eventType) }}</strong>
                      <span class="event-op">{{ operatorLabel(event.operatorType) }}</span>
                    </div>
                    <div v-if="event.remark" class="event-remark">{{ event.remark }}</div>
                  </div>
                </el-timeline-item>
                <el-timeline-item v-if="!ticket?.events?.length" type="info">暂无处理记录</el-timeline-item>
              </el-timeline>
            </el-card>
          </div>

          <!-- 右侧 -->
          <div class="right-col">
            <!-- 提交人信息 -->
            <el-card class="side-card">
              <template #header>
                <div class="card-title"><el-icon><User /></el-icon>提交人</div>
              </template>
              <div class="user-info">
                <el-avatar :size="48">{{ (ticket?.requesterName || '用户').charAt(0) }}</el-avatar>
                <div class="user-detail">
                  <div class="user-name">{{ ticket?.requesterName || '用户' + ticket?.userId }}</div>
                  <div class="user-id">用户ID: {{ ticket?.userId }}</div>
                </div>
              </div>
            </el-card>
          </div>
        </div>
      </template>
    </div>

    <!-- 调整优先级弹窗 -->
    <el-dialog v-model="priorityDialog.visible" title="调整优先级" width="400px">
      <el-form label-width="80px">
        <el-form-item label="优先级">
          <el-radio-group v-model="priorityDialog.priority">
            <el-radio value="LOW">低</el-radio>
            <el-radio value="NORMAL">普通</el-radio>
            <el-radio value="HIGH">高</el-radio>
            <el-radio value="URGENT">紧急</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="priorityDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="confirmPriority">确认</el-button>
      </template>
    </el-dialog>

    <!-- 改派弹窗 -->
    <el-dialog v-model="assignDialog.visible" title="改派工单" width="400px">
      <el-form label-width="80px">
        <el-form-item label="处理人">
          <el-select v-model="assignDialog.agentId" placeholder="请选择处理人" style="width: 100%">
            <el-option
              v-for="agent in agentList"
              :key="agent.id"
              :label="agent.realName || agent.username"
              :value="agent.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="confirmAssign">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Tickets, Clock, User, Warning, FolderChecked
} from '@element-plus/icons-vue'
import {
  getAdminTicket, updateAdminTicket, listAgents,
  statusColor, statusLabel, priorityColor, priorityLabel
} from '../api/ticket'
import type { Ticket, TicketEvent } from '../api/ticket'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const loadError = ref(false)
const ticket = ref<Ticket | null>(null)
const actionLoading = ref(false)

const priorityDialog = reactive({
  visible: false,
  priority: 'NORMAL'
})

const assignDialog = reactive({
  visible: false,
  agentId: null as number | null
})
const agentList = ref<any[]>([])

const reversedEvents = computed(() => [...(ticket.value?.events || [])].reverse())

const isTimeout = computed(() => {
  if (!ticket.value?.timeoutAt) return false
  return new Date(ticket.value.timeoutAt) < new Date()
    && ticket.value.status !== 'COMPLETED'
    && ticket.value.status !== 'REJECTED'
    && ticket.value.status !== 'ARCHIVED'
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
    ticket.value = await getAdminTicket(id)
  } catch (e) {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

function goBack() { router.back() }

async function handleAction(action: string) {
  if (!ticket.value) return
  if (action === 'priority') {
    priorityDialog.priority = ticket.value.priority
    priorityDialog.visible = true
  } else if (action === 'assign') {
    await loadAgents()
    assignDialog.agentId = ticket.value.assigneeAgentId || null
    assignDialog.visible = true
  } else if (action === 'archive') {
    ElMessageBox.confirm('确认归档该工单吗？归档后不可恢复。', '提示', { type: 'warning' })
      .then(async () => {
        actionLoading.value = true
        try {
          await updateAdminTicket(ticket.value!.id, { status: 'ARCHIVED' })
          ElMessage.success('归档成功')
          loadDetail()
        } catch (e: any) {
          ElMessage.error(e.message || '归档失败')
        } finally {
          actionLoading.value = false
        }
      }).catch(() => {})
  }
}

async function loadAgents() {
  try {
    const res = await listAgents({ page: 1, size: 100, status: 'ACTIVE' })
    agentList.value = res.records || []
  } catch (e) {
    ElMessage.error('加载坐席列表失败')
  }
}

async function confirmPriority() {
  if (!ticket.value) return
  actionLoading.value = true
  try {
    await updateAdminTicket(ticket.value.id, { priority: priorityDialog.priority })
    ElMessage.success('优先级已调整')
    priorityDialog.visible = false
    loadDetail()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    actionLoading.value = false
  }
}

async function confirmAssign() {
  if (!ticket.value || !assignDialog.agentId) {
    ElMessage.warning('请选择处理人')
    return
  }
  actionLoading.value = true
  try {
    await updateAdminTicket(ticket.value.id, { assigneeAgentId: assignDialog.agentId })
    ElMessage.success('改派成功')
    assignDialog.visible = false
    loadDetail()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    actionLoading.value = false
  }
}

function eventLabel(type: string) {
  const map: Record<string, string> = {
    CREATE: '工单创建', ASSIGN: '工单分配', ACCEPT: '客服接单', PROCESS: '开始处理',
    FOLLOW: '持续跟进', COMPLETE: '工单完结', REJECT: '工单驳回',
    ADMIN_UPDATE: '管理员更新', TRANSFER: '工单转派', ARCHIVE: '工单归档'
  }
  return map[type] || type
}

function operatorLabel(type: string) {
  const map: Record<string, string> = { USER: '用户', AGENT: '客服', ADMIN: '管理员', SYSTEM: '系统' }
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
.detail-page { min-height: 600px; }

.error-card {
  border-radius: 12px;
  margin-bottom: 16px;
}

.top-bar {
  border-radius: 12px;
  margin-bottom: 16px;
}
.top-bar :deep(.el-card__body) { padding: 16px 20px; }

.top-title {
  display: flex;
  align-items: center;
  gap: 12px;
}
.ticket-id { font-size: 18px; font-weight: 600; color: #1e293b; }

.top-actions { display: flex; gap: 8px; }

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 280px;
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
  margin: 0 0 16px;
  color: #1e293b;
}

.desc-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #f1f5f9;
}
.section-title { font-weight: 600; color: #334155; margin-bottom: 8px; }
.desc-content {
  color: #475569;
  line-height: 1.8;
  background: #f8fafc;
  padding: 12px 16px;
  border-radius: 8px;
  white-space: pre-wrap;
}

.timeout-text { color: #dc2626; }

.event-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.event-op { font-size: 12px; color: #64748b; }
.event-remark { color: #475569; font-size: 13px; margin-top: 4px; }

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-name { font-weight: 500; color: #1e293b; }
.user-id { font-size: 12px; color: #94a3b8; }

@media (max-width: 900px) {
  .detail-grid { grid-template-columns: 1fr; }
}
</style>
