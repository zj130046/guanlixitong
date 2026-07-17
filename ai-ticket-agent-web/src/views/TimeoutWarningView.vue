<template>
  <div class="warning-page">
    <!-- 预警统计卡片 -->
    <div class="stat-row">
      <div class="warn-card danger">
        <el-icon :size="32"><Warning /></el-icon>
        <div class="warn-info">
          <div class="warn-value">{{ dangerCount }}</div>
          <div class="warn-label">已超时</div>
        </div>
      </div>
      <div class="warn-card warning">
        <el-icon :size="32"><Clock /></el-icon>
        <div class="warn-info">
          <div class="warn-value">{{ warningCount }}</div>
          <div class="warn-label">4小时内超时</div>
        </div>
      </div>
      <div class="warn-card info">
        <el-icon :size="32"><Tickets /></el-icon>
        <div class="warn-info">
          <div class="warn-value">{{ totalPending }}</div>
          <div class="warn-label">待处理总数</div>
        </div>
      </div>
    </div>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>超时预警工单</span>
          <el-button @click="loadData">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" style="width: 100%" stripe>
        <el-table-column prop="id" label="工单号" width="90">
          <template #default="{ row }">#{{ row.id }}</template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="priority" label="优先级" width="90">
          <template #default="{ row }">
            <el-tag :type="priorityColor[row.priority]" size="small">
              {{ priorityLabel[row.priority] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusColor[row.status]" size="small">
              {{ statusLabel[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="timeoutAt" label="超时时间" width="170">
          <template #default="{ row }">
            <span :class="{ 'timeout-danger': isDanger(row.timeoutAt) }">
              {{ formatTime(row.timeoutAt) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="剩余时间" width="130">
          <template #default="{ row }">
            <el-tag :type="remainType(row.timeoutAt)" effect="plain" size="small">
              {{ remainTime(row.timeoutAt) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="goDetail(row.id)">
              去处理
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无超时预警工单，继续加油！💪" />
        </template>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Warning, Clock, Tickets, Refresh } from '@element-plus/icons-vue'
import {
  timeoutWarnings, statusColor, statusLabel, priorityColor, priorityLabel
} from '../api/ticket'
import type { Ticket } from '../api/ticket'

const router = useRouter()
const loading = ref(false)
const tableData = ref<Ticket[]>([])

const dangerCount = computed(() =>
  tableData.value.filter(t => isDanger(t.timeoutAt)).length
)
const warningCount = computed(() =>
  tableData.value.filter(t => !isDanger(t.timeoutAt)).length
)
const totalPending = computed(() => tableData.value.length)

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    tableData.value = await timeoutWarnings()
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

function isDanger(time?: string) {
  if (!time) return false
  return new Date(time) < new Date()
}

function remainType(time?: string) {
  if (!time) return 'info'
  const diff = new Date(time).getTime() - Date.now()
  if (diff < 0) return 'danger'
  if (diff < 3600000) return 'warning'
  return 'info'
}

function remainTime(time?: string) {
  if (!time) return '-'
  const diff = new Date(time).getTime() - Date.now()
  if (diff < 0) {
    const abs = Math.abs(diff)
    const hours = Math.floor(abs / 3600000)
    const mins = Math.floor((abs % 3600000) / 60000)
    return `已超时 ${hours}h${mins}m`
  }
  const hours = Math.floor(diff / 3600000)
  const mins = Math.floor((diff % 3600000) / 60000)
  return `${hours}h${mins}m`
}

function goDetail(id: number) {
  router.push(`/tickets/${id}`)
}

function formatTime(time?: string) {
  if (!time) return '-'
  const d = new Date(time)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped>
.stat-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.warn-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.warn-card.danger { border-left: 4px solid #ef4444; color: #ef4444; }
.warn-card.warning { border-left: 4px solid #f59e0b; color: #f59e0b; }
.warn-card.info { border-left: 4px solid #3b82f6; color: #3b82f6; }

.warn-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
}
.warn-label {
  margin-top: 6px;
  font-size: 14px;
  color: #64748b;
}

.table-card { border-radius: 12px; }
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #1e293b;
}

.timeout-danger { color: #dc2626; font-weight: 500; }
</style>
