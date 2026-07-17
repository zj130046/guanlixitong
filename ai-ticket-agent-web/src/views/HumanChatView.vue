<template>
  <div class="chat-page">
    <!-- 左侧会话列表 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <span class="title">待处理会话</span>
        <el-badge :value="pendingCount" class="item" type="danger">
          <el-icon><Bell /></el-icon>
        </el-badge>
      </div>
      <div class="conv-list">
        <div
          v-for="conv in conversations"
          :key="conv.id"
          :class="['conv-item', { active: activeId === conv.id }]"
          @click="selectConv(conv)"
        >
          <el-avatar :size="40">
            {{ (conv.userName || '用户').charAt(0) }}
          </el-avatar>
          <div class="conv-info">
            <div class="conv-top">
              <span class="conv-name">{{ conv.userName || '用户' + conv.userId }}</span>
              <span class="conv-time">{{ formatTime(conv.updatedAt) }}</span>
            </div>
            <div class="conv-preview">{{ conv.preview || '用户请求人工服务' }}</div>
          </div>
          <el-tag v-if="conv.status === 'WAITING_AGENT'" size="small" type="warning" class="conv-tag">
            待接入
          </el-tag>
        </div>
        <el-empty v-if="!conversations.length" description="暂无会话" :image-size="60" />
      </div>
    </div>

    <!-- 右侧聊天区 -->
    <div class="chat-main">
      <div v-if="!activeConv" class="empty-chat">
        <el-empty description="请选择一个会话开始处理" />
      </div>
      <template v-else>
        <div class="chat-header">
          <div class="header-left">
            <el-avatar :size="36">
              {{ (activeConv.userName || '用户').charAt(0) }}
            </el-avatar>
            <div class="header-info">
              <div class="header-name">{{ activeConv.userName || '用户' + activeConv.userId }}</div>
              <div class="header-sub">
                <el-tag size="small" :type="activeConv.status === 'WAITING_AGENT' ? 'warning' : 'success'">
                  {{ activeConv.status === 'WAITING_AGENT' ? '等待接入' : '对话中' }}
                </el-tag>
                <span v-if="activeConv.ticketId" class="ticket-link">
                  关联工单：#{{ activeConv.ticketId }}
                </span>
              </div>
            </div>
          </div>
          <div class="header-right">
            <el-button size="small" @click="viewTicket">
              <el-icon><Tickets /></el-icon>
              查看工单
            </el-button>
          </div>
        </div>

        <div ref="messagesRef" class="messages">
          <div
            v-for="(msg, idx) in messages"
            :key="idx"
            :class="['msg-row', msg.senderType === 'USER' ? 'user' : 'agent']"
          >
            <el-avatar :size="36" class="avatar">
              {{ msg.senderType === 'USER' ? (activeConv.userName || '用').charAt(0) : '客' }}
            </el-avatar>
            <div class="msg-bubble">
              <div class="msg-sender">
                {{ msg.senderType === 'USER' ? '用户' : msg.senderType === 'AI' ? 'AI 助手' : '客服' }}
              </div>
              <div class="msg-content">{{ msg.content }}</div>
              <div class="msg-time">{{ formatTime(msg.createdAt) }}</div>
            </div>
          </div>
        </div>

        <!-- 快捷回复 -->
        <div class="quick-replies">
          <span class="quick-label">快捷回复：</span>
          <el-tag
            v-for="(q, idx) in quickReplies"
            :key="idx"
            class="quick-tag"
            effect="plain"
            @click="useQuickReply(q)"
          >
            {{ q }}
          </el-tag>
        </div>

        <div class="input-area">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="2"
            placeholder="输入回复内容..."
            @keydown.enter.ctrl="sendMessage"
          />
          <div class="input-actions">
            <span class="tip">按 Ctrl + Enter 发送</span>
            <el-button type="primary" :disabled="!inputText.trim()" @click="sendMessage">
              发送
            </el-button>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, reactive, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Bell, Tickets } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const conversations = ref<any[]>([])
const activeId = ref<number | null>(null)
const activeConv = ref<any>(null)
const messages = ref<any[]>([])
const inputText = ref('')
const messagesRef = ref<HTMLElement | null>(null)
const pendingCount = ref(0)

const quickReplies = [
  '您好，请问有什么可以帮您？',
  '正在为您核实，请稍候...',
  '感谢您的耐心等待',
  '问题已记录，我们会尽快处理',
  '请问还有其他问题吗？'
]

onMounted(() => {
  // 模拟会话列表（真实项目从 API 获取）
  conversations.value = [
    {
      id: 1, userId: 1, userName: '张三',
      status: 'WAITING_AGENT', ticketId: 1001,
      preview: '我的订单什么时候发货？',
      updatedAt: new Date(Date.now() - 300000).toISOString()
    },
    {
      id: 2, userId: 2, userName: '李四',
      status: 'IN_PROGRESS', ticketId: 1002,
      preview: '好的，谢谢！',
      updatedAt: new Date(Date.now() - 600000).toISOString()
    }
  ]
  pendingCount.value = conversations.value.filter(c => c.status === 'WAITING_AGENT').length

  // 根据 URL 参数自动选中会话
  const paramId = Number(route.params.conversationId)
  if (paramId) {
    const conv = conversations.value.find(c => c.id === paramId)
    if (conv) {
      selectConv(conv)
    }
  }
})

function selectConv(conv: any) {
  activeId.value = conv.id
  activeConv.value = conv
  // 同步 URL
  if (route.params.conversationId !== String(conv.id)) {
    router.replace(`/chat/${conv.id}`)
  }
  // 模拟消息
  messages.value = [
    { senderType: 'USER', content: '你好，我想咨询一下订单问题', createdAt: new Date(Date.now() - 1800000).toISOString() },
    { senderType: 'AI', content: '您好！我是AI助手，请问您遇到了什么问题呢？', createdAt: new Date(Date.now() - 1780000).toISOString() },
    { senderType: 'USER', content: '我的订单什么时候发货？已经等了3天了', createdAt: new Date(Date.now() - 1700000).toISOString() },
    { senderType: 'AI', content: '正在为您查询订单信息...', createdAt: new Date(Date.now() - 1680000).toISOString() },
    { senderType: 'USER', content: '太慢了，我要转人工', createdAt: new Date(Date.now() - 1600000).toISOString() },
    { senderType: 'SYSTEM', content: '已转人工，工单号：#1001', createdAt: new Date(Date.now() - 1500000).toISOString() },
  ]
  scrollToBottom()
}

function useQuickReply(text: string) {
  inputText.value = text
}

function sendMessage() {
  if (!inputText.value.trim()) return
  messages.value.push({
    senderType: 'AGENT',
    content: inputText.value,
    createdAt: new Date().toISOString()
  })
  inputText.value = ''
  scrollToBottom()
  ElMessage.success('消息已发送')
}

function viewTicket() {
  if (activeConv.value?.ticketId) {
    router.push(`/tickets/${activeConv.value.ticketId}`)
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

function formatTime(time: string) {
  if (!time) return ''
  const d = new Date(time)
  const diff = Date.now() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped>
.chat-page {
  display: flex;
  height: calc(100vh - 120px);
  min-height: 560px;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}

.sidebar {
  width: 280px;
  background: #f8fafc;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e2e8f0;
  font-weight: 600;
  color: #1e293b;
}

.conv-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.conv-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background 0.2s;
}
.conv-item:hover { background: #e2e8f0; }
.conv-item.active { background: #dbeafe; }

.conv-info { flex: 1; min-width: 0; }
.conv-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}
.conv-name { font-weight: 500; color: #1e293b; }
.conv-time { color: #94a3b8; font-size: 11px; }
.conv-preview {
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.conv-tag { margin-top: 4px; }

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.empty-chat {
  flex: 1;
  display: grid;
  place-items: center;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid #f1f5f9;
}
.header-left { display: flex; gap: 12px; align-items: center; }
.header-name { font-weight: 600; color: #1e293b; }
.header-sub {
  display: flex;
  gap: 10px;
  align-items: center;
  font-size: 12px;
  margin-top: 3px;
}
.ticket-link { color: #3b82f6; cursor: pointer; }

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #fafbfc;
}

.msg-row {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  align-items: flex-start;
}
.msg-row.user { flex-direction: row-reverse; }

.avatar { flex-shrink: 0; }

.msg-bubble {
  max-width: 65%;
  background: #fff;
  padding: 10px 14px;
  border-radius: 10px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.06);
}
.msg-row.user .msg-bubble { background: #dbeafe; }
.msg-row.agent .msg-bubble { background: #dcfce7; }

.msg-sender { font-size: 11px; color: #94a3b8; margin-bottom: 4px; }
.msg-content { font-size: 14px; color: #1e293b; line-height: 1.6; white-space: pre-wrap; }
.msg-time { font-size: 11px; color: #cbd5e1; margin-top: 6px; text-align: right; }

.quick-replies {
  padding: 10px 20px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}
.quick-label { font-size: 12px; color: #94a3b8; }
.quick-tag { cursor: pointer; transition: all 0.2s; }
.quick-tag:hover { background: #eff6ff; }

.input-area {
  padding: 12px 20px 16px;
  border-top: 1px solid #e2e8f0;
  background: #fff;
}

.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}
.tip { font-size: 12px; color: #94a3b8; }
</style>
