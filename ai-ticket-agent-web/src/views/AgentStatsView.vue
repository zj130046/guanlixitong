<template>
  <div class="stats-page">
    <!-- 个人数据卡 -->
    <div class="stat-grid">
      <div class="stat-card primary">
        <div class="stat-value">{{ stats.completedTickets || 0 }}</div>
        <div class="stat-label">本月完结</div>
      </div>
      <div class="stat-card success">
        <div class="stat-value">{{ stats.completionRate || 0 }}%</div>
        <div class="stat-label">完结率</div>
      </div>
      <div class="stat-card warning">
        <div class="stat-value">{{ stats.satisfactionScore || '4.8' }}</div>
        <div class="stat-label">平均满意度</div>
      </div>
      <div class="stat-card info">
        <div class="stat-value">{{ stats.processingTickets || 0 }}</div>
        <div class="stat-label">处理中</div>
      </div>
    </div>

    <div class="chart-grid">
      <!-- 处理趋势 -->
      <el-card class="chart-card">
        <template #header>
          <div class="card-header">
            <span>个人工单处理趋势</span>
            <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
              <el-radio-button :value="7">近7天</el-radio-button>
              <el-radio-button :value="15">近15天</el-radio-button>
              <el-radio-button :value="30">近30天</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <div ref="trendChartRef" class="chart"></div>
      </el-card>

      <!-- 工单分类分布 -->
      <el-card class="chart-card">
        <template #header><span>处理工单分类分布</span></template>
        <div ref="barChartRef" class="chart"></div>
      </el-card>
    </div>

    <!-- 效率指标（示例数据，后续对接真实 API） -->
    <el-card class="eff-card">
      <template #header>
        <span class="card-title">效率指标
          <el-tag size="small" type="info" style="margin-left:8px">示例数据</el-tag>
        </span>
      </template>
      <el-row :gutter="24">
        <el-col :span="6">
          <div class="eff-item">
            <div class="eff-label">平均响应时间</div>
            <div class="eff-value">--<span class="eff-unit">分钟</span></div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="eff-item">
            <div class="eff-label">平均处理时长</div>
            <div class="eff-value">--<span class="eff-unit">小时</span></div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="eff-item">
            <div class="eff-label">首次解决率</div>
            <div class="eff-value">--<span class="eff-unit">%</span></div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="eff-item">
            <div class="eff-label">返工率</div>
            <div class="eff-value">--<span class="eff-unit">%</span></div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, reactive } from 'vue'
import * as echarts from 'echarts'
import { agentOverview, ticketTrend, categoryDistribution, getSatisfactionStats } from '../api/ticket'

const trendDays = ref(7)
const stats = reactive({
  completedTickets: 0,
  completionRate: 0,
  satisfactionScore: 0 as number | string,
  processingTickets: 0
})

const trendChartRef = ref<HTMLElement>()
const barChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null
let barChart: echarts.ECharts | null = null

onMounted(async () => {
  await loadStats()
  nextTick(() => {
    trendChart = echarts.init(trendChartRef.value!)
    barChart = echarts.init(barChartRef.value!)
    loadTrend()
    loadCategory()
  })
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  barChart?.dispose()
})

function handleResize() {
  trendChart?.resize()
  barChart?.resize()
}

async function loadStats() {
  try {
    const [overviewData, satData] = await Promise.all([
      agentOverview(),
      getSatisfactionStats()
    ])
    stats.completedTickets = Number(overviewData.completedTickets || 0)
    stats.processingTickets = Number(overviewData.processingTickets || 0)
    const total = Number(overviewData.totalTickets || 0)
    stats.completionRate = total > 0 ? Math.round(stats.completedTickets * 100 / total) : 0
    // 从全局满意度统计获取平均分
    const avg = satData?.averageScore
    stats.satisfactionScore = avg != null ? avg : '—'
  } catch (e) {
    stats.satisfactionScore = '—'
  }
}

async function loadTrend() {
  if (!trendChart) return
  try {
    const data = await ticketTrend(trendDays.value)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['承接工单', '完结工单'] },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: data.map((d: any) => d.date) },
      yAxis: { type: 'value' },
      series: [
        {
          name: '承接工单', type: 'line', smooth: true,
          data: data.map((d: any) => d.created),
          areaStyle: { opacity: 0.15 }, itemStyle: { color: '#3b82f6' }
        },
        {
          name: '完结工单', type: 'line', smooth: true,
          data: data.map((d: any) => d.completed),
          areaStyle: { opacity: 0.15 }, itemStyle: { color: '#10b981' }
        }
      ]
    })
  } catch (e) { /* ignore */ }
}

async function loadCategory() {
  if (!barChart) return
  try {
    const data = await categoryDistribution()
    barChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: data.map((d: any) => d.name) },
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
</script>

<style scoped>
.stats-page { min-height: 600px; }

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  border-top: 4px solid transparent;
}
.stat-card.primary { border-top-color: #3b82f6; }
.stat-card.success { border-top-color: #10b981; }
.stat-card.warning { border-top-color: #f59e0b; }
.stat-card.info { border-top-color: #6366f1; }

.stat-value {
  font-size: 32px;
  font-weight: 700;
}
.stat-card.primary .stat-value { color: #3b82f6; }
.stat-card.success .stat-value { color: #10b981; }
.stat-card.warning .stat-value { color: #f59e0b; }
.stat-card.info .stat-value { color: #6366f1; }

.stat-label {
  margin-top: 6px;
  font-size: 14px;
  color: #64748b;
}

.chart-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.chart-card { border-radius: 12px; }
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #1e293b;
}

.chart { height: 300px; width: 100%; }

.eff-card { border-radius: 12px; }
.card-title { font-weight: 600; color: #1e293b; }

.eff-item {
  text-align: center;
  padding: 20px 0;
}
.eff-label { font-size: 13px; color: #64748b; margin-bottom: 8px; }
.eff-value {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
}
.eff-unit {
  font-size: 14px;
  font-weight: normal;
  color: #64748b;
  margin-left: 4px;
}

@media (max-width: 900px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .chart-grid { grid-template-columns: 1fr; }
}
</style>
