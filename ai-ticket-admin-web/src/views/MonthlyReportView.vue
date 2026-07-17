<template>
  <div class="monthly-report">
    <el-card class="header-card">
      <div class="header-content">
        <div>
          <h2>月度服务报表</h2>
          <p>系统整体服务质量与效率数据汇总</p>
        </div>
        <div class="month-selector">
          <el-date-picker v-model="selectedMonth" type="month" placeholder="选择月份" value-format="YYYY-MM" />
          <el-button type="primary" @click="handleExport">
            <el-icon><Download /></el-icon>导出报表
          </el-button>
        </div>
      </div>
    </el-card>

    <div class="stat-grid">
      <div class="stat-card blue">
        <div class="stat-value">2856</div>
        <div class="stat-label">总咨询量</div>
        <div class="stat-change up">↑ 12.5% 环比</div>
      </div>
      <div class="stat-card green">
        <div class="stat-value">82.3%</div>
        <div class="stat-label">AI 回复率</div>
        <div class="stat-change up">↑ 5.2% 环比</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-value">91.6%</div>
        <div class="stat-label">工单完结率</div>
        <div class="stat-change up">↑ 3.8% 环比</div>
      </div>
      <div class="stat-card purple">
        <div class="stat-value">4.7</div>
        <div class="stat-label">平均满意度</div>
        <div class="stat-change up">↑ 0.2 环比</div>
      </div>
    </div>

    <div class="chart-row">
      <el-card class="chart-card">
        <template #header><span>咨询量趋势</span></template>
        <div ref="c1" class="chart"></div>
      </el-card>
      <el-card class="chart-card">
        <template #header><span>工单处理效率</span></template>
        <div ref="c2" class="chart"></div>
      </el-card>
    </div>

    <div class="chart-row">
      <el-card class="chart-card">
        <template #header><span>工单分类占比</span></template>
        <div ref="c3" class="chart"></div>
      </el-card>
      <el-card class="chart-card">
        <template #header><span>坐席业绩排行</span></template>
        <div ref="c4" class="chart"></div>
      </el-card>
      <el-card class="chart-card">
        <template #header><span>满意度分布</span></template>
        <div ref="c5" class="chart"></div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'

const selectedMonth = ref('2024-06')
const c1 = ref<HTMLElement>()
const c2 = ref<HTMLElement>()
const c3 = ref<HTMLElement>()
const c4 = ref<HTMLElement>()
const c5 = ref<HTMLElement>()
let charts: echarts.ECharts[] = []

onMounted(() => {
  nextTick(initCharts)
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach(c => c && c.dispose())
  charts = []
})
function handleResize() { charts.forEach(c => c.resize()) }

function initCharts() {
  const colors = ['#3b82f6','#10b981','#f59e0b','#ef4444','#8b5cf6','#06b6d4']

  // 咨询量
  const chart1 = echarts.init(c1.value!)
  chart1.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['咨询量', '转人工'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['6/1','6/5','6/10','6/15','6/20','6/25','6/30'] },
    yAxis: { type: 'value' },
    series: [
      { name: '咨询量', type: 'line', smooth: true, data: [120, 180, 240, 200, 280, 310, 260], areaStyle: { opacity: 0.1 }, itemStyle: { color: colors[0] } },
      { name: '转人工', type: 'line', smooth: true, data: [20, 35, 40, 30, 45, 50, 42], itemStyle: { color: colors[2] } }
    ]
  })
  charts.push(chart1)

  // 工单效率
  const chart2 = echarts.init(c2.value!)
  chart2.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['新增', '完结'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['第1周','第2周','第3周','第4周'] },
    yAxis: { type: 'value' },
    series: [
      { name: '新增', type: 'bar', data: [120, 145, 160, 130], itemStyle: { color: '#60a5fa', borderRadius: [4,4,0,0] } },
      { name: '完结', type: 'bar', data: [100, 130, 150, 140], itemStyle: { color: '#34d399', borderRadius: [4,4,0,0] } }
    ]
  })
  charts.push(chart2)

  // 分类
  const chart3 = echarts.init(c3.value!)
  chart3.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', right: 10, top: 'center' },
    series: [{
      type: 'pie', radius: ['45%','70%'], center: ['35%','50%'],
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      data: [
        { value: 180, name: '校园报修' },
        { value: 150, name: '业务咨询' },
        { value: 90, name: '投诉建议' },
        { value: 120, name: '产品售后' },
        { value: 60, name: '其他' },
      ], color: colors
    }]
  })
  charts.push(chart3)

  // 排行
  const chart4 = echarts.init(c4.value!)
  chart4.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: ['赵工','王工','李工','张工','刘工'].reverse() },
    series: [{
      type: 'bar', data: [85, 98, 110, 125, 140].reverse(),
      label: { show: true, position: 'right' },
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0,0,1,0,[{offset:0,color:'#10b981'},{offset:1,color:'#34d399'}]),
        borderRadius: [0,4,4,0]
      }, barWidth: '50%'
    }]
  })
  charts.push(chart4)

  // 满意度
  const chart5 = echarts.init(c5.value!)
  chart5.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['1星','2星','3星','4星','5星'] },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar', data: [5, 10, 45, 120, 200],
      label: { show: true, position: 'top' },
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'#fbbf24'},{offset:1,color:'#f59e0b'}]),
        borderRadius: [4,4,0,0]
      }, barWidth: '50%'
    }]
  })
  charts.push(chart5)
}

function handleExport() { ElMessage.success('报表导出中，请稍候...') }
</script>

<style scoped>
.monthly-report { min-height: 600px; }
.header-card {
  border-radius: 12px; margin-bottom: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff; border: none;
}
.header-card :deep(.el-card__body) { padding: 24px; }
.header-content { display: flex; justify-content: space-between; align-items: center; }
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
.stat-change { display: flex; align-items: center; gap: 4px; font-size: 12px; margin-top: 8px; }
.stat-change.up { color: #10b981; }
.stat-change.down { color: #ef4444; }

.chart-row {
  display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px;
}
.chart-row:last-child { grid-template-columns: 1fr 1fr 1fr; }
.chart-card { border-radius: 12px; }
.chart { height: 280px; width: 100%; }

@media (max-width: 900px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .chart-row, .chart-row:last-child { grid-template-columns: 1fr; }
}
</style>
