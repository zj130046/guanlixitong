<template>
  <div class="satisfaction-stats">
    <div class="stat-grid">
      <div class="stat-card primary">
        <div class="stat-main">
          <div class="stat-value">4.7</div>
          <div class="stat-label">综合满意度</div>
        </div>
        <el-rate :model-value="4.7" disabled allow-half show-score />
      </div>
      <div class="stat-card success">
        <div class="stat-value">380</div>
        <div class="stat-label">总评价数</div>
      </div>
      <div class="stat-card warning">
        <div class="stat-value">89.2%</div>
        <div class="stat-label">好评率</div>
      </div>
      <div class="stat-card info">
        <div class="stat-value">12</div>
        <div class="stat-label">待处理评价</div>
      </div>
    </div>

    <div class="chart-row">
      <el-card class="chart-card">
        <template #header><span>评分分布</span></template>
        <div ref="barChartRef" class="chart"></div>
      </el-card>
      <el-card class="chart-card">
        <template #header><span>满意度趋势（近30天）</span></template>
        <div ref="trendChartRef" class="chart"></div>
      </el-card>
    </div>

    <el-card class="list-card">
      <template #header>
        <div class="card-header">
          <span>最新评价</span>
          <el-radio-group v-model="filterScore" size="small">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="5">5星</el-radio-button>
            <el-radio-button value="1">1星</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div class="review-list">
        <div v-for="item in reviews" :key="item.id" class="review-item">
          <div class="review-header">
            <el-avatar :size="36">{{ item.userName.charAt(0) }}</el-avatar>
            <div class="review-info">
              <div class="review-user">{{ item.userName }}</div>
              <div class="review-meta">
                <el-rate :model-value="item.score" disabled size="small" />
                <span class="review-time">{{ item.time }}</span>
              </div>
            </div>
            <el-tag v-if="item.ticketId" size="small" effect="plain" type="info">
              工单 #{{ item.ticketId }}
            </el-tag>
          </div>
          <div v-if="item.comment" class="review-content">{{ item.comment }}</div>
        </div>
        <el-empty v-if="!reviews.length" description="暂无评价" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, reactive } from 'vue'
import * as echarts from 'echarts'

const filterScore = ref('')
const barChartRef = ref<HTMLElement>()
const trendChartRef = ref<HTMLElement>()
let charts: echarts.ECharts[] = []

const reviews = ref([
  { id: 1, userName: '用户001', score: 5, time: '2024-06-15 14:30', ticketId: 1023, comment: '响应速度很快，问题解决得也很专业，非常满意！' },
  { id: 2, userName: '用户002', score: 4, time: '2024-06-15 10:20', ticketId: 1022, comment: '整体还不错，就是等待时间稍微有点长' },
  { id: 3, userName: '用户003', score: 5, time: '2024-06-14 16:45', ticketId: 1020, comment: '客服很耐心，问题解决得很彻底' },
  { id: 4, userName: '用户004', score: 3, time: '2024-06-14 09:10', ticketId: 1018, comment: '沟通了好几次才解决，希望能提升效率' },
  { id: 5, userName: '用户005', score: 5, time: '2024-06-13 20:30', ticketId: 1015, comment: '晚上也有人工客服在，太方便了' },
])

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
  // 评分分布柱状图
  const chart1 = echarts.init(barChartRef.value!)
  chart1.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['1星', '2星', '3星', '4星', '5星'] },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar', data: [5, 12, 38, 125, 200],
      label: { show: true, position: 'top' },
      itemStyle: {
        color: (params: any) => {
          const colors = ['#ef4444', '#f97316', '#f59e0b', '#84cc16', '#22c55e']
          return colors[params.dataIndex]
        },
        borderRadius: [4,4,0,0]
      },
      barWidth: '50%'
    }]
  })
  charts.push(chart1)

  // 趋势
  const chart2 = echarts.init(trendChartRef.value!)
  chart2.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: Array.from({length:30},(_,i)=>`${i+1}日`) },
    yAxis: { type: 'value', min: 4, max: 5 },
    series: [{
      type: 'line', smooth: true,
      data: Array.from({length:30},()=> (4.3 + Math.random()*0.6).toFixed(1)),
      areaStyle: { opacity: 0.15 },
      itemStyle: { color: '#f59e0b' }
    }]
  })
  charts.push(chart2)
}
</script>

<style scoped>
.satisfaction-stats { min-height: 600px; }

.stat-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 16px;
}
.stat-card {
  background: #fff; border-radius: 12px; padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.stat-card.primary .stat-value { color: #3b82f6; }
.stat-card.success .stat-value { color: #10b981; }
.stat-card.warning .stat-value { color: #f59e0b; }
.stat-card.info .stat-value { color: #6366f1; }
.stat-value { font-size: 32px; font-weight: 700; }
.stat-label { font-size: 14px; color: #64748b; margin-top: 4px; }

.chart-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; }
.chart-card, .list-card { border-radius: 12px; }
.card-header { display: flex; justify-content: space-between; align-items: center; font-weight: 600; color: #1e293b; }
.chart { height: 300px; width: 100%; }

.review-list { display: flex; flex-direction: column; gap: 12px; }
.review-item {
  padding: 16px; background: #f8fafc; border-radius: 10px;
}
.review-header { display: flex; align-items: center; gap: 12px; }
.review-info { flex: 1; }
.review-user { font-weight: 500; color: #1e293b; }
.review-meta { display: flex; align-items: center; gap: 10px; margin-top: 4px; }
.review-time { font-size: 12px; color: #94a3b8; }
.review-content {
  margin-top: 10px; padding-left: 48px; color: #475569; line-height: 1.7;
}

@media (max-width: 900px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .chart-row { grid-template-columns: 1fr; }
}
</style>
