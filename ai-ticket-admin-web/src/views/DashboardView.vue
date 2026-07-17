<template>
  <div class="dashboard">
    <!-- 顶部数据卡 -->
    <div class="stat-grid">
      <div class="stat-card" v-for="(item, idx) in statCards" :key="idx" :class="item.color">
        <div class="stat-icon">
          <el-icon :size="28"><component :is="item.icon" /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ item.value }}</div>
          <div class="stat-label">{{ item.label }}</div>
          <div class="stat-trend" v-if="item.trend">
            <el-icon><TrendCharts /></el-icon>
            <span>{{ item.trend }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 图表行 1 -->
    <div class="chart-row">
      <el-card class="chart-card large">
        <template #header>
          <div class="card-header">
            <span>咨询量 & 工单趋势</span>
            <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
              <el-radio-button :value="7">近7天</el-radio-button>
              <el-radio-button :value="15">近15天</el-radio-button>
              <el-radio-button :value="30">近30天</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <div ref="trendChartRef" class="chart"></div>
      </el-card>

      <el-card class="chart-card">
        <template #header><span>工单状态分布</span></template>
        <div ref="pieChartRef" class="chart"></div>
      </el-card>
    </div>

    <!-- 图表行 2 -->
    <div class="chart-row">
      <el-card class="chart-card">
        <template #header><span>工单分类分布</span></template>
        <div ref="barChartRef" class="chart"></div>
      </el-card>

      <el-card class="chart-card">
        <template #header><span>坐席工作量 TOP5</span></template>
        <div ref="rankChartRef" class="chart"></div>
      </el-card>

      <el-card class="chart-card">
        <template #header><span>满意度评分分布</span></template>
        <div ref="satisfactionChartRef" class="chart"></div>
      </el-card>
    </div>

    <!-- 最新工单 -->
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>最新工单</span>
          <el-button type="primary" text @click="$router.push('/tickets')">
            查看全部
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </template>
      <el-table :data="recentTickets" size="default" style="width: 100%">
        <el-table-column prop="id" label="工单号" width="90">
          <template #default="{ row }">#{{ row.id }}</template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.category || '未分类' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="80">
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
        <el-table-column prop="requesterName" label="提交人" width="100" />
        <el-table-column prop="createdAt" label="创建时间" width="150">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, reactive } from 'vue'
import * as echarts from 'echarts'
import {
  ChatLineRound, Tickets, UserFilled, Star,
  TrendCharts, ArrowRight
} from '@element-plus/icons-vue'
import {
  statisticsOverview, consultationTrend, ticketStatusStats,
  ticketCategoryStats, agentRanking, satisfactionStats,
  adminTickets, statusColor, statusLabel, priorityColor, priorityLabel
} from '../api/ticket'
import type { Ticket } from '../api/ticket'

const trendDays = ref(7)
const recentTickets = ref<Ticket[]>([])

const statCards = reactive([
  { label: '今日咨询量', value: 0, color: 'blue', icon: ChatLineRound, trend: '+12%' },
  { label: 'AI 回复率', value: '0%', color: 'green', icon: 'Odometer', trend: '+5%' },
  { label: '总工单数', value: 0, color: 'orange', icon: Tickets, trend: '+8%' },
  { label: '满意度', value: 0, color: 'purple', icon: Star, trend: '+0.2' },
])

const chartRefs = {
  trend: null as echarts.ECharts | null,
  pie: null as echarts.ECharts | null,
  bar: null as echarts.ECharts | null,
  rank: null as echarts.ECharts | null,
  satisfaction: null as echarts.ECharts | null,
}

const trendChartRef = ref<HTMLElement>()
const pieChartRef = ref<HTMLElement>()
const barChartRef = ref<HTMLElement>()
const rankChartRef = ref<HTMLElement>()
const satisfactionChartRef = ref<HTMLElement>()

onMounted(async () => {
  await loadOverview()
  await loadRecentTickets()

  nextTick(() => {
    chartRefs.trend = echarts.init(trendChartRef.value!)
    chartRefs.pie = echarts.init(pieChartRef.value!)
    chartRefs.bar = echarts.init(barChartRef.value!)
    chartRefs.rank = echarts.init(rankChartRef.value!)
    chartRefs.satisfaction = echarts.init(satisfactionChartRef.value!)

    loadTrend()
    loadPieChart()
    loadBarChart()
    loadRankChart()
    loadSatisfactionChart()
  })

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  Object.values(chartRefs).forEach(c => c?.dispose())
})

function handleResize() {
  Object.values(chartRefs).forEach(c => c?.resize())
}

async function loadOverview() {
  try {
    const data = await statisticsOverview()
    statCards[0].value = data.todayConsultations || 0
    statCards[1].value = (data.aiReplyRate || 0) + '%'
    statCards[2].value = data.totalTickets || 0
    statCards[3].value = data.satisfactionScore || 0
  } catch (e) { /* ignore */ }
}

async function loadRecentTickets() {
  try {
    const res = await adminTickets({ page: 1, size: 5 })
    recentTickets.value = res.records
  } catch (e) { /* ignore */ }
}

async function loadTrend() {
  if (!chartRefs.trend) return
  try {
    const data = await consultationTrend(trendDays.value)
    chartRefs.trend.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['总咨询', 'AI回复', '转人工'] },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: data.map((d: any) => d.date) },
      yAxis: { type: 'value' },
      series: [
        {
          name: '总咨询', type: 'line', smooth: true,
          data: data.map((d: any) => d.total),
          itemStyle: { color: '#3b82f6' },
          areaStyle: { opacity: 0.1 }
        },
        {
          name: 'AI回复', type: 'line', smooth: true,
          data: data.map((d: any) => d.aiReplied),
          itemStyle: { color: '#10b981' },
          areaStyle: { opacity: 0.1 }
        },
        {
          name: '转人工', type: 'line', smooth: true,
          data: data.map((d: any) => d.transferred),
          itemStyle: { color: '#f59e0b' },
          areaStyle: { opacity: 0.1 }
        }
      ]
    })
  } catch (e) { /* ignore */ }
}

async function loadPieChart() {
  if (!chartRefs.pie) return
  try {
    const data = await ticketStatusStats()
    chartRefs.pie.setOption({
      tooltip: { trigger: 'item' },
      legend: { orient: 'vertical', right: 10, top: 'center' },
      series: [{
        type: 'pie',
        radius: ['45%', '75%'],
        center: ['35%', '50%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
        data: data.map((d: any) => ({ name: d.name, value: d.value })),
        color: ['#3b82f6', '#f59e0b', '#10b981', '#6366f1', '#ef4444', '#8b5cf6', '#06b6d4']
      }]
    })
  } catch (e) { /* ignore */ }
}

async function loadBarChart() {
  if (!chartRefs.bar) return
  try {
    const data = await ticketCategoryStats()
    chartRefs.bar.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'category',
        data: data.map((d: any) => d.name),
        axisLabel: { rotate: 30, fontSize: 11 }
      },
      yAxis: { type: 'value' },
      series: [{
        type: 'bar',
        data: data.map((d: any) => d.value),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#60a5fa' },
            { offset: 1, color: '#3b82f6' }
          ]),
          borderRadius: [4, 4, 0, 0]
        },
        barWidth: '50%'
      }]
    })
  } catch (e) { /* ignore */ }
}

async function loadRankChart() {
  if (!chartRefs.rank) return
  try {
    const data = await agentRanking(5)
    chartRefs.rank.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'value' },
      yAxis: {
        type: 'category',
        data: data.map((d: any) => d.agentName).reverse(),
        axisLabel: { fontSize: 12 }
      },
      series: [{
        type: 'bar',
        data: data.map((d: any) => d.completed).reverse(),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#10b981' },
            { offset: 1, color: '#34d399' }
          ]),
          borderRadius: [0, 4, 4, 0]
        },
        barWidth: '60%',
        label: { show: true, position: 'right', fontSize: 12 }
      }]
    })
  } catch (e) { /* ignore */ }
}

async function loadSatisfactionChart() {
  if (!chartRefs.satisfaction) return
  try {
    const data = await satisfactionStats()
    const dist = data.distribution || [
      { score: 5, count: 28 },
      { score: 4, count: 15 },
      { score: 3, count: 5 },
      { score: 2, count: 2 },
      { score: 1, count: 1 }
    ]
    chartRefs.satisfaction.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: dist.map((d: any) => d.score + '星') },
      yAxis: { type: 'value' },
      series: [{
        type: 'bar',
        data: dist.map((d: any) => d.count),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#fbbf24' },
            { offset: 1, color: '#f59e0b' }
          ]),
          borderRadius: [4, 4, 0, 0]
        },
        barWidth: '50%',
        label: { show: true, position: 'top' }
      }]
    })
  } catch (e) { /* ignore */ }
}

function formatTime(time: string) {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped>
.dashboard {
  min-height: 600px;
}

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
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  position: relative;
  overflow: hidden;
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  color: #fff;
  flex-shrink: 0;
}
.stat-card.blue .stat-icon { background: linear-gradient(135deg, #60a5fa, #3b82f6); }
.stat-card.green .stat-icon { background: linear-gradient(135deg, #34d399, #10b981); }
.stat-card.orange .stat-icon { background: linear-gradient(135deg, #fbbf24, #f59e0b); }
.stat-card.purple .stat-icon { background: linear-gradient(135deg, #a78bfa, #8b5cf6); }

.stat-content { flex: 1; }
.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.2;
}
.stat-label {
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
}
.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #10b981;
  margin-top: 4px;
}

.chart-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}
.chart-row:nth-child(3) {
  grid-template-columns: 1fr 1fr 1fr;
}

.chart-card { border-radius: 12px; }
.chart-card.large { border-radius: 12px; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #1e293b;
}

.chart { height: 300px; width: 100%; }

.table-card { border-radius: 12px; }

@media (max-width: 1200px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .chart-row, .chart-row:nth-child(3) { grid-template-columns: 1fr; }
}
</style>
