<template>
  <div class="chat-page">
    <!-- 左侧会话列表 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <el-button type="primary" class="new-chat-btn" @click="startNewChat">
          <el-icon><ChatDotRound /></el-icon>
          新对话
        </el-button>
      </div>
      <div class="conversation-list">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          :class="['conv-item', { active: currentConvId === conv.id }]"
          @click="switchConversation(conv.id)"
        >
          <div class="conv-title">{{ conv.title || '新对话' }}</div>
          <div class="conv-meta">
            <span :class="['status-tag', conv.status]">{{ statusLabel(conv.status) }}</span>
            <div class="conv-actions" @click.stop>
              <span class="conv-time">{{ formatTime(conv.updatedAt) }}</span>
              <el-button
                type="danger"
                size="small"
                link
                class="delete-btn"
                @click.stop="handleDelete(conv)"
              >
                删除
              </el-button>
            </div>
          </div>
        </div>
        <div v-if="conversations.length === 0" class="empty-conv">
          暂无历史会话
        </div>
      </div>
    </div>

    <!-- 右侧聊天区 -->
    <div class="chat-main">
      <!-- 快捷问题 -->
      <div v-if="messages.length <= 1" class="quick-questions">
        <div class="quick-title">常见问题</div>
        <div class="quick-grid">
          <div
            v-for="(q, idx) in quickQuestions"
            :key="idx"
            class="quick-item"
            @click="sendQuick(q)"
          >
            {{ q }}
          </div>
        </div>
      </div>

      <!-- 消息列表 -->
      <div ref="messagesRef" class="messages">
        <div
          v-for="(msg, idx) in messages"
          :key="idx"
          :class="['msg-row', msg.role]"
        >
          <div class="avatar">
            <el-avatar :size="36" :class="msg.role === 'user' ? 'user-avatar' : 'ai-avatar'">
              {{ msg.role === 'user' ? '我' : 'AI' }}
            </el-avatar>
          </div>
          <div class="msg-bubble">
            <div class="msg-content" v-html="formatContent(msg.content)"></div>
            <div v-if="msg.matchType" class="msg-tag">
              {{ msg.matchType === 'semantic' ? '语义匹配' : msg.matchType === 'keyword' ? '关键词匹配' : '知识库' }}
            </div>
          </div>
        </div>

        <!-- 正在输入动画 -->
        <div v-if="isTyping" class="msg-row ai">
          <div class="avatar">
            <el-avatar :size="36" class="ai-avatar">AI</el-avatar>
          </div>
          <div class="msg-bubble typing">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="input-area">
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="2"
          placeholder="请描述您的问题，例如：宿舍水管漏水怎么办？"
          maxlength="500"
          show-word-limit
          @keydown.enter.ctrl="handleSend"
        />
        <div class="input-actions">
          <el-button @click="handleTransfer" :disabled="isTyping">
            <el-icon><UserFilled /></el-icon>
            转人工
          </el-button>
          <el-button type="primary" @click="handleSend" :disabled="isTyping || !inputText.trim()">
            <el-icon><Promotion /></el-icon>
            发送
          </el-button>
        </div>
        <div class="input-tip">按 Ctrl + Enter 快捷发送 · 7×24 小时智能服务</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, UserFilled, Promotion } from '@element-plus/icons-vue'
import {
  createConversation, listConversations, listMessages, deleteConversation,
  transferHuman, streamSendMessage
} from '../api/chat'
import type { ChatConversation, ChatMessage } from '../api/chat'

const route = useRoute()

const conversations = ref<ChatConversation[]>([])
const currentConvId = ref<number | null>(null)
const messages = ref<Array<{ role: 'user' | 'ai'; content: string; matchType?: string }>>([])
const inputText = ref('')
const isTyping = ref(false)
const messagesRef = ref<HTMLElement | null>(null)

const quickQuestions = [
  '宿舍水管漏水怎么办？',
  '校园网怎么连接？',
  '食堂开放时间是几点？',
  '图书馆怎么借书？',
  '快递在哪里取？',
  '怎么申请报修？'
]

onMounted(async () => {
  await loadConversations()
  // 优先从 URL 参数中获取会话 ID（从历史记录页跳转过来的场景）
  const convIdFromUrl = route.query.convId
  if (convIdFromUrl && !isNaN(Number(convIdFromUrl))) {
    const id = Number(convIdFromUrl)
    if (conversations.value.some(c => c.id === id)) {
      switchConversation(id)
      return
    }
  }
  // 有历史会话则恢复最近一个，没有才新建
  if (conversations.value.length > 0) {
    switchConversation(conversations.value[0].id)
  } else {
    startNewChat()
  }
})

async function loadConversations() {
  try {
    const data = await listConversations()
    conversations.value = data as unknown as ChatConversation[]
  } catch (e) {
    // 忽略
  }
}

async function startNewChat() {
  try {
    const res = await createConversation() as any
    currentConvId.value = res.conversationId
    messages.value = [{
      role: 'ai',
      content: '您好！我是智能客服助手 🤖 有什么可以帮您的吗？您可以直接描述问题，或从下方常见问题中选择。'
    }]
    scrollToBottom()
  } catch (e) {
    ElMessage.error('创建会话失败')
  }
}

function switchConversation(id: number) {
  currentConvId.value = id
  loadMessages(id)
}

async function handleDelete(conv: ChatConversation) {
  try {
    await ElMessageBox.confirm(
      `确定要删除会话「${conv.title || '新对话'}」吗？删除后无法恢复。`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteConversation(conv.id)
    ElMessage.success('已删除')
    // 如果删除的是当前会话，清空或切换到第一个
    if (currentConvId.value === conv.id) {
      const remaining = conversations.value.filter(c => c.id !== conv.id)
      if (remaining.length > 0) {
        switchConversation(remaining[0].id)
      } else {
        startNewChat()
      }
    }
    loadConversations()
  } catch (e: any) {
    if (e !== 'cancel' && e?.message !== 'cancel') {
      ElMessage.error(e?.message || '删除失败')
    }
  }
}

async function loadMessages(id: number) {
  try {
    const data = await listMessages(id) as ChatMessage[]
    messages.value = data.map(m => ({
      role: m.senderType === 'USER' ? 'user' : 'ai',
      content: m.content
    }))
    scrollToBottom()
  } catch (e) {
    ElMessage.error('加载消息失败')
  }
}

function sendQuick(question: string) {
  inputText.value = question
  handleSend()
}

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || isTyping.value) return

  const userMsg = { role: 'user' as const, content: text }
  messages.value.push(userMsg)
  inputText.value = ''
  isTyping.value = true

  // 先加一个空的 AI 消息占位
  const aiMsgIndex = messages.value.length
  messages.value.push({ role: 'ai' as const, content: '', matchType: '' as string | undefined })

  await nextTick()
  scrollToBottom()

  try {
    const result = await streamSendMessage(text, currentConvId.value || undefined, (token) => {
      messages.value[aiMsgIndex].content += token
      scrollToBottom()
    })
    if (result.conversationId && !currentConvId.value) {
      currentConvId.value = result.conversationId
    }
    messages.value[aiMsgIndex].content = result.answer
    isTyping.value = false
    scrollToBottom()
    loadConversations()
  } catch (e: any) {
    messages.value[aiMsgIndex].content = '抱歉，AI 服务暂时不可用，请稍后再试。'
    isTyping.value = false
    ElMessage.error(e.message || '发送失败')
  }
}

async function handleTransfer() {
  try {
    await ElMessageBox.confirm(
      '确认要转人工客服吗？系统将为您生成工单，客服人员会尽快与您联系。',
      '转人工确认',
      { confirmButtonText: '确认转人工', cancelButtonText: '取消', type: 'info' }
    )

    const res = await transferHuman(currentConvId.value || undefined) as any
    ElMessage.success(`已转人工，工单号：${res.ticketId}`)

    messages.value.push({
      role: 'ai',
      content: `📋 已为您生成人工工单，工单号：<strong>#${res.ticketId}</strong><br>客服人员会尽快处理您的问题，请在「我的工单」中查看进度。`
    })
    scrollToBottom()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('转人工失败')
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

function formatContent(content: string) {
  return content
    .replace(/\n/g, '<br>')
    .replace(/_(.+?)_/g, '<em>$1</em>')
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

function formatTime(time: string) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return `${d.getMonth() + 1}/${d.getDate()}`
}
</script>

<style scoped>
.chat-page {
  display: flex;
  height: calc(100vh - 120px);
  min-height: 600px;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}

.sidebar {
  width: 260px;
  background: #f8fafc;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #e2e8f0;
}

.new-chat-btn {
  width: 100%;
  justify-content: center;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.conv-item {
  padding: 12px 14px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background 0.2s;
}
.conv-item:hover { background: #e2e8f0; }
.conv-item.active { background: #dbeafe; }

.conv-title {
  font-weight: 500;
  font-size: 14px;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
}

.conv-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #64748b;
}

.conv-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.delete-btn {
  opacity: 0;
  transition: opacity 0.2s;
  padding: 0 !important;
  font-size: 12px;
}

.conv-item:hover .delete-btn {
  opacity: 1;
}

.status-tag {
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  background: #e0f2fe;
  color: #0369a1;
}
.status-tag.WAITING_AGENT { background: #fef3c7; color: #b45309; }
.status-tag.TRANSFERRED { background: #dcfce7; color: #15803d; }

.empty-conv {
  text-align: center;
  color: #94a3b8;
  padding: 40px 20px;
  font-size: 13px;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.quick-questions {
  padding: 16px 24px;
  border-bottom: 1px solid #f1f5f9;
  background: #fafbfc;
}
.quick-title { font-weight: 600; color: #334155; margin-bottom: 12px; font-size: 14px; }
.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
.quick-item {
  padding: 10px 14px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s;
}
.quick-item:hover {
  border-color: #3b82f6;
  color: #2563eb;
  background: #eff6ff;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: #fafbfc;
}

.msg-row {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  align-items: flex-start;
}
.msg-row.user {
  flex-direction: row-reverse;
}

.avatar { flex-shrink: 0; }
.user-avatar { background: #3b82f6; }
.ai-avatar { background: #10b981; }

.msg-bubble {
  max-width: 70%;
  background: #fff;
  padding: 12px 16px;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.06);
  line-height: 1.7;
  font-size: 14px;
  color: #1e293b;
  position: relative;
}
.msg-row.user .msg-bubble {
  background: #dbeafe;
}

.msg-content { word-break: break-word; }
.msg-content :deep(em) { color: #64748b; font-size: 12px; }

.msg-tag {
  display: inline-block;
  margin-top: 8px;
  padding: 2px 8px;
  background: #f0fdf4;
  color: #15803d;
  border-radius: 10px;
  font-size: 11px;
}

.typing {
  display: flex;
  gap: 4px;
  align-items: center;
  padding: 16px;
}
.typing span {
  width: 8px;
  height: 8px;
  background: #94a3b8;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}
.typing span:nth-child(1) { animation-delay: -0.32s; }
.typing span:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.input-area {
  padding: 16px 24px;
  border-top: 1px solid #e2e8f0;
  background: #fff;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.input-tip {
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
  margin-top: 8px;
}
</style>
