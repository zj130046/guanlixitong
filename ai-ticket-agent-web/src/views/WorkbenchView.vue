<template>
  <div class="workbench">
    <!-- 顶部状态条 -->
    <el-card class="status-bar">
      <div class="status-left">
        <el-avatar :size="48" class="avatar">
          {{ (agentInfo?.realName || agentInfo?.username || '坐席').charAt(0) }}
        </el-avatar>
        <div class="agent-info">
          <h3>{{ agentInfo?.realName || agentInfo?.username || '坐席' }}</h3>
          <div class="agent-meta">
            <el-tag :type="isOnline ? 'success' : 'info'" effect="dark" size="small">
              {{ isOnline ? '在线' : '离线' }}
            </el-tag>
            <span class="role">{{ agentInfo?.role === 'LEADER' ? '组长' : '客服坐席' }}</span>
          </div>
        </div>
      </div>
      <div class="status-right">
        <el-switch
          v-model="isOnline"
          active-text="在线"
          inactive-text="离线"
          :loading="switchLoading"
          @change="handleSwitchStatus"
        />
      </div>
    </el-card>

    <!-- 数据卡片 -->
    <div class="stat-grid">
      <div class="stat-card primary" @click="goPool">
        <div class="stat-value">{{ stats.pendingTickets || 0 }}</div>
        <div class="stat-label">待接工单</div>
        <el-icon class="stat-icon"><Bell /></el-icon>
      </div>
      <div class="stat-card success" @click="goMine">
        <div class="stat-value">{{ stats.completedTickets || 0 }}</div>
        <div class="stat-label">今日完结</div>
        <el-icon class="stat-icon"><CircleCheck /></el-icon>
      </div>
      <div class="stat-card warning" @click="goWarnings">
        <div class="stat-value">{{ timeoutCount }}</div>
        <div class="stat-label">超时预警</div>
        <el-icon class="stat-icon"><Warning /></el-icon>
      </div>
      <div class="stat-card info">
        <div class="stat-value">{{ stats.processingTickets || 0 }}</div>
        <div class="stat-label">处理中</div>
        <el-icon class="stat-icon"><Loading /></el-icon>
      </div>
    </div>

    <div class="content-grid">
      <!-- 工单趋势图 -->
      <el-card class="chart-card">
        <template #header>
          <div class="card-header">
            <span>近 7 日工单趋势</span>
            <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
              <el-radio-button :value="7">7天</el-radio-button>
              <el-radio-button :value="15">15天</el-radio-button>
              <el-radio-button :value="30">30天</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <div ref="trendChartRef" class="chart-container"></div>
      </el-card>

      <!-- 工单状态分布 -->
      <el-card class="chart-card">
        <template #header>
          <span>工单状态分布</span>
        </template>
        <div ref="pieChartRef" class="chart-container pie-chart"></div>
      </el-card>
    </div>

    <!-- 待接工单 + 最近处理 -->
    <div class="content-grid" style="margin-top: 16px">
      <el-card class="list-card">
        <template #header>
          <div class="card-header">
            <span>最新待接工单</span>
            <el-button type="primary" text @click="goPool">查看全部</el-button>
          </div>
        </template>
        <el-table :data="poolTickets" size="default" style="width: 100%">
          <el-table-column prop="id" label="工单号" width="80">
            <template #default="{ row }">#{{ row.id }}</template>
          </el-table-column>
          <el-table-column prop="title" label="标题" show-overflow-tooltip />
          <el-table-column prop="priority" label="优先级" width="90">
            <template #default="{ row }">
              <el-tag :type="priorityColor[row.priority]" size="small">
                {{ priorityLabel[row.priority] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button type="primary" size="small" link @click="handleAccept(row.id)">
                接单
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!poolTickets.length" description="暂无待接工单" :image-size="60" />
      </el-card>

      <el-card class="list-card">
        <template #header>
          <div class="card-header">
            <span>我的工单</span>
            <el-button type="primary" text @click="goMine">查看全部</el-button>
          </div>
        </template>
        <el-table :data="myTicketsList" size="default" style="width: 100%">
          <el-table-column prop="id" label="工单号" width="80">
            <template #default="{ row }">#{{ row.id }}</template>
          </el-table-column>
          <el-table-column prop="title" label="标题" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusColor[row.status]" size="small">
                {{ statusLabel[row.status] }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!myTicketsList.length" description="暂无工单" :image-size="60" />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import {
  Bell, CircleCheck, Warning, Loading
} from '@element-plus/icons-vue'
import {
  ticketPool, myTickets, acceptTicket, timeoutWarnings,
  agentOverview, ticketTrend, statusDistribution,
  goOnline, goOffline, getAgentMe,
  statusColor, statusLabel, priorityColor, priorityLabel
} from '../api/ticket'
import type { Ticket } from '../api/ticket'

const router = useRouter()
const isOnline = ref(false)
const switchLoading = ref(false)
const agentInfo = ref<any>(null)

const stats = ref({
  pendingTickets: 0,
  completedTickets: 0,
  processingTickets: 0
})
const timeoutCount = ref(0)
const poolTickets = ref<Ticket[]>([])
const myTicketsList = ref<Ticket[]>([])
const trendDays = ref(7)

const trendChartRef = ref<HTMLElement>()
const pieChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

onMounted(async () => {
  await loadAgentInfo()
  await loadOverview()
  await loadPool()
  await loadMyTickets()
  await loadTimeoutCount()

  nextTick(() => {
    initTrendChart()
    initPieChart()
    loadTrend()
    loadStatusDistribution()
  })

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  pieChart?.dispose()
})

function handleResize() {
  trendChart?.resize()
  pieChart?.resize()
}

async function loadAgentInfo() {
  try {
    agentInfo.value = await getAgentMe()
    isOnline.value = agentInfo.value?.onlineStatus === 'ONLINE'
  } catch (e) {
    // ignore
  }
}

async function loadOverview() {
  try {
    const data = await agentOverview()
    stats.value = {
      pendingTickets: data.pendingTickets || 0,
      completedTickets: data.completedTickets || 0,
      processingTickets: data.processingTickets || 0
    }
  } catch (e) {
    // ignore
  }
}

async function loadPool() {
  try {
    const res = await ticketPool({ page: 1, size: 5 })
    poolTickets.value = res.records
  } catch (e) { /* ignore */ }
}

async function loadMyTickets() {
  try {
    const res = await myTickets({ page: 1, size: 5 })
    myTicketsList.value = res.records
  } catch (e) { /* ignore */ }
}

async function loadTimeoutCount() {
  try {
    const list = await timeoutWarnings()
    timeoutCount.value = list.length
  } catch (e) { /* ignore */ }
}

async function handleSwitchStatus(val: boolean) {
  switchLoading.value = true
  try {
    if (val) {
      await goOnline()
      ElMessage.success('已上线')
    } else {
      await goOffline()
      ElMessage.success('已下线')
    }
  } catch (e: any) {
    isOnline.value = !val
    ElMessage.error(e.message || '操作失败')
  } finally {
    switchLoading.value = false
  }
}

async function handleAccept(id: number) {
  try {
    await acceptTicket(id)
    ElMessage.success('接单成功')
    loadPool()
    loadMyTickets()
    loadOverview()
  } catch (e: any) {
    ElMessage.error(e.message || '接单失败')
  }
}

function goPool() { router.push('/tickets/pool') }
function goMine() { router.push('/tickets/mine') }
function goWarnings() { router.push('/warnings') }

// ========== 图表 ==========

function initTrendChart() {
  if (!trendChartRef.value) return
  trendChart = echarts.init(trendChartRef.value)
}

async function loadTrend() {
  if (!trendChart) return
  try {
    const data = await ticketTrend(trendDays.value)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['新增工单', '完结工单'], right: 10 },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: data.map((d: any) => d.date)
      },
      yAxis: { type: 'value' },
      series: [
        {
          name: '新增工单',
          type: 'line',
          smooth: true,
          data: data.map((d: any) => d.created),
          areaStyle: { opacity: 0.1 },
          itemStyle: { color: '#3b82f6' }
        },
        {
          name: '完结工单',
          type: 'line',
          smooth: true,
          data: data.map((d: any) => d.completed),
          areaStyle: { opacity: 0.1 },
          itemStyle: { color: '#10b981' }
        }
      ]
    })
  } catch (e) { /* ignore */ }
}

function initPieChart() {
  if (!pieChartRef.value) return
  pieChart = echarts.init(pieChartRef.value)
}

async function loadStatusDistribution() {
  if (!pieChart) return
  try {
    const data = await statusDistribution()
    pieChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { orient: 'vertical', left: 'left' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: {
          label: { show: true, fontSize: 14, fontWeight: 'bold' }
        },
        data: data.map((d: any) => ({
          name: d.name,
          value: d.value
        })),
        color: ['#3b82f6', '#f59e0b', '#10b981', '#6366f1', '#ef4444', '#8b5cf6']
      }]
    })
  } catch (e) { /* ignore */ }
}
</script>

<style scoped>
.workbench {
  min-height: 600px;
}

.status-bar {
  margin-bottom: 16px;
  border-radius: 12px;
}
.status-bar :deep(.el-card__body) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
}

.status-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.avatar { background: linear-gradient(135deg, #60a5fa, #3b82f6); }
.agent-info h3 {
  margin: 0 0 6px 0;
  font-size: 18px;
  color: #1e293b;
}
.agent-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}
.role { font-size: 13px; color: #64748b; }

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  position: relative;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
}
.stat-card.primary .stat-value { color: #3b82f6; }
.stat-card.success .stat-value { color: #10b981; }
.stat-card.warning .stat-value { color: #f59e0b; }
.stat-card.info .stat-value { color: #6366f1; }

.stat-label {
  margin-top: 8px;
  font-size: 14px;
  color: #64748b;
}

.stat-icon {
  position: absolute;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 48px;
  opacity: 0.1;
}
.stat-card.primary .stat-icon { color: #3b82f6; }
.stat-card.success .stat-icon { color: #10b981; }
.stat-card.warning .stat-icon { color: #f59e0b; }
.stat-card.info .stat-icon { color: #6366f1; }

.content-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}

.chart-card, .list-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #1e293b;
}

.chart-container {
  height: 280px;
  width: 100%;
}
.pie-chart { height: 260px; }

@media (max-width: 900px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .content-grid { grid-template-columns: 1fr; }
}
</style>
