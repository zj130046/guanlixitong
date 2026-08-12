<template>
  <div class="monthly-report">
    <el-card class="header-card">
      <div class="header-content">
        <div>
          <h2>月度服务报表</h2>
          <p>系统整体服务质量与效率数据汇总</p>
        </div>
        <div class="month-selector">
          <el-button type="primary" :loading="generating" @click="handleGenerate">
            <el-icon><Refresh /></el-icon>生成本月报表
          </el-button>
          <el-button @click="handleExportReport">
            <el-icon><Download /></el-icon>导出当前报表
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 月度数据卡 -->
    <div class="stat-grid">
      <div class="stat-card blue">
        <div class="stat-value">{{ currentReport?.consultationCount || overviewData.todayConsultations || 0 }}</div>
        <div class="stat-label">总咨询量</div>
      </div>
      <div class="stat-card green">
        <div class="stat-value">{{ currentReport?.aiReplyRate || overviewData.aiReplyRate || 0 }}%</div>
        <div class="stat-label">AI 回复率</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-value">{{ currentReport?.ticketCompletionRate || overviewData.ticketCompletionRate || 0 }}%</div>
        <div class="stat-label">工单完结率</div>
      </div>
      <div class="stat-card purple">
        <div class="stat-value">{{ currentReport?.satisfactionScore || overviewData.satisfactionScore || '—' }}</div>
        <div class="stat-label">平均满意度</div>
      </div>
    </div>

    <!-- 最近报表列表 -->
    <el-card class="table-card" v-if="reports.length">
      <template #header><span>历史月度报表</span></template>
      <el-table :data="reports" size="default" stripe>
        <el-table-column prop="reportMonth" label="报表月份" width="120" />
        <el-table-column prop="consultationCount" label="咨询量" width="100" />
        <el-table-column prop="aiReplyRate" label="AI回复率" width="100">
          <template #default="{ row }">{{ row.aiReplyRate }}%</template>
        </el-table-column>
        <el-table-column prop="manualTransferRate" label="转人工率" width="100">
          <template #default="{ row }">{{ row.manualTransferRate }}%</template>
        </el-table-column>
        <el-table-column prop="ticketCompletionRate" label="工单完结率" width="110">
          <template #default="{ row }">{{ row.ticketCompletionRate }}%</template>
        </el-table-column>
        <el-table-column prop="satisfactionScore" label="满意度" width="100" />
        <el-table-column prop="createdAt" label="生成时间" min-width="160">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 详情图表行 -->
    <div class="chart-row">
      <el-card class="chart-card">
        <template #header><span>咨询量趋势（近30天）</span></template>
        <div ref="c1" class="chart"></div>
      </el-card>
      <el-card class="chart-card">
        <template #header><span>工单分类占比</span></template>
        <div ref="c3" class="chart"></div>
      </el-card>
    </div>

    <div class="chart-row three-col">
      <el-card class="chart-card">
        <template #header><span>满意度分布</span></template>
        <div ref="c5" class="chart"></div>
      </el-card>
      <el-card class="chart-card">
        <template #header><span>坐席业绩排行 TOP5</span></template>
        <div ref="c4" class="chart"></div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { Download, Refresh } from '@element-plus/icons-vue'
import {
  statisticsOverview, consultationTrend, ticketCategoryStats,
  agentRanking, satisfactionStats, listMonthlyReports, generateMonthlyReport
} from '../api/ticket'

const generating = ref(false)
const overviewData = ref<Record<string, any>>({})
const currentReport = ref<any>(null)
const reports = ref<any[]>([])

const c1 = ref<HTMLElement>()
const c3 = ref<HTMLElement>()
const c4 = ref<HTMLElement>()
const c5 = ref<HTMLElement>()
let charts: echarts.ECharts[] = []

onMounted(async () => {
  await loadData()
  nextTick(initCharts)
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach(c => c?.dispose())
})

async function loadData() {
  try {
    const [overview, reportList] = await Promise.all([
      statisticsOverview(),
      listMonthlyReports()
    ])
    overviewData.value = overview
    reports.value = reportList || []
    currentReport.value = (reportList && reportList.length) ? reportList[0] : null
  } catch (e) { /* ignore */ }
}

async function handleGenerate() {
  generating.value = true
  try {
    const report = await generateMonthlyReport()
    currentReport.value = report
    ElMessage.success('本月报表已生成')
    // 刷新列表
    const list = await listMonthlyReports()
    reports.value = list || []
  } catch (e: any) {
    ElMessage.error(e.message || '生成失败')
  } finally { generating.value = false }
}

async function handleExportReport() {
  // 导出为 CSV
  if (!reports.value.length) {
    ElMessage.warning('暂无报表数据')
    return
  }
  const header = '月份,咨询量,AI回复率,转人工率,工单完结率,满意度\n'
  const rows = reports.value.map((r: any) =>
    `${r.reportMonth},${r.consultationCount},${r.aiReplyRate}%,${r.manualTransferRate}%,${r.ticketCompletionRate}%,${r.satisfactionScore}`
  ).join('\n')
  const csv = '﻿' + header + rows
  const blob = new Blob([csv], { type: 'text/csv;charset=UTF-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `月度报表-${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(a.href)
  ElMessage.success('导出成功')
}

function handleResize() { charts.forEach(c => c?.resize()) }

async function initCharts() {
  const colors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4']

  // 咨询趋势
  try {
    const trend = await consultationTrend(30)
    const chart1 = echarts.init(c1.value!)
    chart1.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['总咨询', 'AI回复', '转人工'] },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: trend.map((d: any) => d.date), axisLabel: { rotate: 30, fontSize: 10 } },
      yAxis: { type: 'value' },
      series: [
        { name: '总咨询', type: 'line', smooth: true, data: trend.map((d: any) => d.total), itemStyle: { color: colors[0] }, areaStyle: { opacity: 0.1 } },
        { name: 'AI回复', type: 'line', smooth: true, data: trend.map((d: any) => d.aiReplied), itemStyle: { color: colors[1] }, areaStyle: { opacity: 0.1 } },
        { name: '转人工', type: 'line', smooth: true, data: trend.map((d: any) => d.transferred), itemStyle: { color: colors[2] } }
      ]
    })
    charts.push(chart1)
  } catch (e) { /* ignore */ }

  // 分类占比
  try {
    const catData = await ticketCategoryStats()
    const chart3 = echarts.init(c3.value!)
    chart3.setOption({
      tooltip: { trigger: 'item' },
      legend: { orient: 'vertical', right: 10, top: 'center' },
      series: [{
        type: 'pie', radius: ['45%', '70%'], center: ['35%', '50%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
        data: catData.map((d: any) => ({ name: d.name, value: d.value })),
        color: colors
      }]
    })
    charts.push(chart3)
  } catch (e) { /* ignore */ }

  // 坐席排行
  try {
    const rankData = await agentRanking(5)
    const chart4 = echarts.init(c4.value!)
    chart4.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'value' },
      yAxis: { type: 'category', data: rankData.map((d: any) => d.agentName).reverse() },
      series: [{
        type: 'bar', data: rankData.map((d: any) => d.completed).reverse(),
        label: { show: true, position: 'right' },
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#10b981' }, { offset: 1, color: '#34d399' }]),
          borderRadius: [0, 4, 4, 0]
        }, barWidth: '50%'
      }]
    })
    charts.push(chart4)
  } catch (e) { /* ignore */ }

  // 满意度
  try {
    const satData = await satisfactionStats()
    const dist = satData.distribution || []
    const chart5 = echarts.init(c5.value!)
    chart5.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: dist.map((d: any) => d.score + '星') },
      yAxis: { type: 'value' },
      series: [{
        type: 'bar', data: dist.map((d: any) => d.count),
        label: { show: true, position: 'top' },
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#fbbf24' }, { offset: 1, color: '#f59e0b' }]),
          borderRadius: [4, 4, 0, 0]
        }, barWidth: '50%'
      }]
    })
    charts.push(chart5)
  } catch (e) { /* ignore */ }
}

function formatTime(time: string) {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped>
.monthly-report { min-height: 600px; }
.header-card {
  border-radius: 12px; margin-bottom: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff; border: none;
}
.header-card :deep(.el-card__body) { padding: 24px; }
.header-content { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.header-content h2 { margin: 0 0 4px; color: #fff; font-size: 22px; }
.header-content p { margin: 0; color: rgba(255,255,255,0.8); font-size: 13px; }
.month-selector { display: flex; gap: 10px; align-items: center; }

.stat-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 16px;
}
.stat-card {
  background: #fff; border-radius: 12px; padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06); border-left: 4px solid transparent;
}
.stat-card.blue { border-left-color: #3b82f6; }
.stat-card.green { border-left-color: #10b981; }
.stat-card.orange { border-left-color: #f59e0b; }
.stat-card.purple { border-left-color: #8b5cf6; }
.stat-value { font-size: 28px; font-weight: 700; }
.stat-card.blue .stat-value { color: #3b82f6; }
.stat-card.green .stat-value { color: #10b981; }
.stat-card.orange .stat-value { color: #f59e0b; }
.stat-card.purple .stat-value { color: #8b5cf6; }
.stat-label { font-size: 13px; color: #64748b; margin-top: 4px; }

.table-card { border-radius: 12px; margin-bottom: 16px; }

.chart-row {
  display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px;
}
.chart-row.three-col { grid-template-columns: 1fr 1fr; }
.chart-card { border-radius: 12px; }
.chart { height: 280px; width: 100%; }

@media (max-width: 900px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .chart-row, .chart-row.three-col { grid-template-columns: 1fr; }
}
</style>
