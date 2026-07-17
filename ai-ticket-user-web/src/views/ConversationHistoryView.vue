<template>
  <div class="history-page">
    <el-card class="history-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">历史会话</span>
          <el-button type="primary" @click="goNewChat">
            <el-icon><ChatDotRound /></el-icon>
            新对话
          </el-button>
        </div>
      </template>

      <div v-loading="loading" class="conversation-list">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          class="conv-card"
          @click="viewConversation(conv.id)"
        >
          <div class="conv-header">
            <div class="conv-title">
              <el-icon class="conv-icon"><ChatLineSquare /></el-icon>
              <span>{{ getTitle(conv) }}</span>
            </div>
            <el-tag :type="statusTagType(conv.status)" size="small">
              {{ statusLabel(conv.status) }}
            </el-tag>
          </div>
          <div class="conv-preview">
            {{ getPreview(conv) }}
          </div>
          <div class="conv-footer">
            <span class="conv-time">
              <el-icon><Clock /></el-icon>
              {{ formatTime(conv.updatedAt) }}
            </span>
            <span v-if="conv.transferredTicketId" class="conv-ticket">
              关联工单：#{{ conv.transferredTicketId }}
            </span>
            <div class="conv-actions" @click.stop>
              <el-button type="danger" link size="small" @click.stop="handleDelete(conv)">
                删除
              </el-button>
              <el-button type="primary" link @click.stop="viewConversation(conv.id)">
                查看
                <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </div>
        </div>

        <el-empty v-if="!loading && !conversations.length" description="暂无历史会话">
          <el-button type="primary" @click="goNewChat">开始对话</el-button>
        </el-empty>
      </div>
    </el-card>

    <!-- 会话详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      title="会话详情"
      size="500px"
      :before-close="closeDrawer"
    >
      <div v-if="currentMessages.length" class="message-history">
        <div
          v-for="(msg, idx) in currentMessages"
          :key="idx"
          :class="['msg-row', msg.senderType === 'USER' ? 'user' : 'ai']"
        >
          <div class="msg-bubble">
            <div class="msg-sender">{{ senderLabel(msg.senderType) }}</div>
            <div class="msg-content">{{ msg.content }}</div>
            <div class="msg-time">{{ formatTime(msg.createdAt) }}</div>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无消息记录" />

      <template #footer>
        <div style="text-align: right">
          <el-button @click="continueChat">
            <el-icon><ChatDotRound /></el-icon>
            继续对话
          </el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ChatDotRound, ChatLineSquare, Clock, ArrowRight
} from '@element-plus/icons-vue'
import { listConversations, listMessages, deleteConversation } from '../api/chat'
import type { ChatConversation, ChatMessage } from '../api/chat'

const router = useRouter()
const loading = ref(false)
const conversations = ref<ChatConversation[]>([])
const drawerVisible = ref(false)
const currentMessages = ref<ChatMessage[]>([])
const currentConvId = ref<number | null>(null)

onMounted(loadConversations)

async function loadConversations() {
  loading.value = true
  try {
    const data = await listConversations()
    conversations.value = data as unknown as ChatConversation[]
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}

function getTitle(conv: ChatConversation) {
  return `会话 #${conv.id}`
}

function getPreview(conv: ChatConversation) {
  return conv.status === 'WAITING_AGENT'
    ? '已转人工客服，点击查看详情'
    : 'AI 智能客服为您服务'
}

function statusLabel(status: string) {
  const map: Record<string, string> = {
    AI_SERVING: 'AI 服务中',
    WAITING_AGENT: '待人工',
    TRANSFERRED: '已转人工',
    CLOSED: '已结束'
  }
  return map[status] || status
}

function statusTagType(status: string) {
  const map: Record<string, string> = {
    AI_SERVING: '',
    WAITING_AGENT: 'warning',
    TRANSFERRED: 'success',
    CLOSED: 'info'
  }
  return map[status] || ''
}

async function viewConversation(id: number) {
  currentConvId.value = id
  drawerVisible.value = true
  currentMessages.value = []
  try {
    const data = await listMessages(id) as ChatMessage[]
    currentMessages.value = data
  } catch (e) {
    // ignore
  }
}

function closeDrawer() {
  drawerVisible.value = false
  currentMessages.value = []
}

function goNewChat() {
  router.push('/chat')
}

function continueChat() {
  if (currentConvId.value) {
    router.push(`/chat?convId=${currentConvId.value}`)
  } else {
    router.push('/chat')
  }
}

async function handleDelete(conv: ChatConversation) {
  try {
    await ElMessageBox.confirm(
      `确定要删除会话「${getTitle(conv)}」吗？删除后无法恢复。`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteConversation(conv.id)
    ElMessage.success('已删除')
    if (currentConvId.value === conv.id) {
      closeDrawer()
    }
    loadConversations()
  } catch (e: any) {
    if (e !== 'cancel' && e?.message !== 'cancel') {
      ElMessage.error(e?.message || '删除失败')
    }
  }
}

function senderLabel(type: string) {
  const map: Record<string, string> = {
    USER: '我',
    AI: 'AI 客服',
    SYSTEM: '系统提示',
    AGENT: '人工客服'
  }
  return map[type] || type
}

function formatTime(time: string) {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped>
.history-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-title { font-weight: 600; color: #1e293b; }

.conversation-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.conv-card {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 16px 18px;
  cursor: pointer;
  transition: all 0.2s;
}
.conv-card:hover {
  border-color: #3b82f6;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.15);
  transform: translateY(-1px);
}

.conv-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.conv-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: #1e293b;
}
.conv-icon { color: #3b82f6; }

.conv-preview {
  color: #64748b;
  font-size: 13px;
  margin-bottom: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-footer {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 12px;
  color: #94a3b8;
  padding-top: 10px;
  border-top: 1px solid #f1f5f9;
}
.conv-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
}
.conv-time {
  display: flex;
  align-items: center;
  gap: 4px;
}
.conv-ticket {
  color: #3b82f6;
}

.message-history {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.msg-row {
  display: flex;
}
.msg-row.user { justify-content: flex-end; }

.msg-bubble {
  max-width: 80%;
  background: #f8fafc;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 13px;
}
.msg-row.user .msg-bubble {
  background: #dbeafe;
}

.msg-sender {
  font-size: 11px;
  color: #94a3b8;
  margin-bottom: 4px;
}
.msg-content {
  color: #1e293b;
  line-height: 1.6;
  white-space: pre-wrap;
}
.msg-time {
  font-size: 11px;
  color: #cbd5e1;
  margin-top: 6px;
  text-align: right;
}
</style>
