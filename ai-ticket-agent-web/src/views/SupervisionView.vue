<template>
  <div class="supervision-page">
    <!-- 组内概览 -->
    <div class="stat-grid">
      <div class="stat-card primary">
        <div class="stat-value">{{ stats.total || 0 }}</div>
        <div class="stat-label">组内总工单</div>
      </div>
      <div class="stat-card warning">
        <div class="stat-value">{{ stats.pending || 0 }}</div>
        <div class="stat-label">待处理</div>
      </div>
      <div class="stat-card danger">
        <div class="stat-value">{{ stats.timeout || 0 }}</div>
        <div class="stat-label">超时工单</div>
      </div>
      <div class="stat-card success">
        <div class="stat-value">{{ stats.completionRate || 0 }}%</div>
        <div class="stat-label">组完结率</div>
      </div>
    </div>

    <div class="content-grid">
      <!-- 坐席工作量排行 -->
      <el-card class="rank-card">
        <template #header>
          <div class="card-header">
            <span>坐席工作量排行</span>
            <el-radio-group v-model="rankPeriod" size="small">
              <el-radio-button value="today">今日</el-radio-button>
              <el-radio-button value="week">本周</el-radio-button>
              <el-radio-button value="month">本月</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <div class="rank-list">
          <div
            v-for="(item, idx) in rankList"
            :key="item.agentId"
            class="rank-item"
          >
            <div :class="['rank-num', `rank-${idx + 1}`]">{{ idx + 1 }}</div>
            <el-avatar :size="40">{{ item.agentName.charAt(0) }}</el-avatar>
            <div class="rank-info">
              <div class="rank-name">{{ item.agentName }}</div>
              <div class="rank-bar">
                <div
                  class="rank-bar-inner"
                  :style="{ width: (item.completed / (rankList[0]?.completed || 1) * 100) + '%' }"
                ></div>
              </div>
            </div>
            <div class="rank-count">
              <strong>{{ item.completed }}</strong>
              <span>单</span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 低效工单 -->
      <el-card class="low-card">
        <template #header>
          <div class="card-header">
            <span class="low-title">
              ⚠️ 低效工单督办
              <el-tag size="small" type="danger">{{ lowEffTickets.length }}</el-tag>
            </span>
          </div>
        </template>
        <div class="low-list">
          <div v-for="t in lowEffTickets" :key="t.id" class="low-item">
            <div class="low-info">
              <div class="low-title-text">#{{ t.id }} {{ t.title }}</div>
              <div class="low-meta">
                <el-tag size="small" type="warning">{{ t.assignee }}</el-tag>
                <span class="low-time">处理时长：{{ t.duration }}</span>
              </div>
            </div>
            <el-button type="primary" size="small" plain @click="handleSupervise(t.id)">
              督办
            </el-button>
          </div>
          <el-empty v-if="!lowEffTickets.length" description="暂无低效工单" :image-size="60" />
        </div>
      </el-card>
    </div>

    <!-- 组内工单列表 -->
    <el-card class="table-card" style="margin-top: 16px">
      <template #header>
        <div class="card-header">
          <span>组内工单</span>
          <el-select v-model="filterStatus" placeholder="状态筛选" clearable size="small" style="width: 120px">
            <el-option label="待处理" value="pending" />
            <el-option label="处理中" value="processing" />
            <el-option label="已完结" value="completed" />
          </el-select>
        </div>
      </template>
      <el-table :data="groupTickets" style="width: 100%" size="default">
        <el-table-column prop="id" label="工单号" width="80">
          <template #default="{ row }">#{{ row.id }}</template>
        </el-table-column>
        <el-table-column prop="title" label="标题" show-overflow-tooltip />
        <el-table-column prop="assignee" label="处理人" width="100" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusColor[row.status]" size="small">
              {{ statusLabel[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="goDetail(row.id)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { statusColor, statusLabel } from '../api/ticket'

const router = useRouter()

const stats = reactive({
  total: 28,
  pending: 8,
  timeout: 2,
  completionRate: 72
})

const rankPeriod = ref('today')
const rankList = ref([
  { agentId: 1, agentName: '张工', completed: 15 },
  { agentId: 2, agentName: '李工', completed: 12 },
  { agentId: 3, agentName: '王工', completed: 9 },
  { agentId: 4, agentName: '赵工', completed: 6 },
])

const lowEffTickets = ref([
  { id: 1023, title: '校园网络故障报修', assignee: '赵工', duration: '48小时' },
  { id: 1025, title: '宿舍空调维修申请', assignee: '王工', duration: '36小时' },
])

const filterStatus = ref('')
const groupTickets = ref([
  { id: 1001, title: '宿舍水管漏水', assignee: '张工', status: 'COMPLETED' },
  { id: 1002, title: '校园网连接问题', assignee: '李工', status: 'PROCESSING' },
  { id: 1003, title: '食堂菜品建议', assignee: '王工', status: 'ACCEPTED' },
  { id: 1004, title: '图书馆占座问题', assignee: '赵工', status: 'ASSIGNED' },
])

function handleSupervise(id: number) {
  ElMessage.success(`已发起对工单 #${id} 的督办，已通知处理人`)
}

function goDetail(id: number) {
  router.push(`/tickets/${id}`)
}
</script>

<style scoped>
.supervision-page { min-height: 600px; }

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
.stat-card.warning { border-top-color: #f59e0b; }
.stat-card.danger { border-top-color: #ef4444; }
.stat-card.success { border-top-color: #10b981; }

.stat-value {
  font-size: 32px;
  font-weight: 700;
}
.stat-card.primary .stat-value { color: #3b82f6; }
.stat-card.warning .stat-value { color: #f59e0b; }
.stat-card.danger .stat-value { color: #ef4444; }
.stat-card.success .stat-value { color: #10b981; }

.stat-label { margin-top: 6px; font-size: 14px; color: #64748b; }

.content-grid {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 16px;
}

.rank-card, .low-card, .table-card { border-radius: 12px; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #1e293b;
}

.rank-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f8fafc;
}

.rank-num {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  background: #94a3b8;
}
.rank-num.rank-1 { background: #f59e0b; }
.rank-num.rank-2 { background: #94a3b8; }
.rank-num.rank-3 { background: #d97706; }

.rank-info {
  flex: 1;
  min-width: 0;
}
.rank-name { font-weight: 500; color: #1e293b; font-size: 14px; margin-bottom: 6px; }
.rank-bar {
  height: 6px;
  background: #e2e8f0;
  border-radius: 3px;
  overflow: hidden;
}
.rank-bar-inner {
  height: 100%;
  background: linear-gradient(90deg, #60a5fa, #3b82f6);
  border-radius: 3px;
  transition: width 0.3s;
}

.rank-count {
  text-align: right;
  color: #64748b;
  font-size: 13px;
}
.rank-count strong {
  font-size: 18px;
  color: #1e293b;
  margin-right: 2px;
}

.low-title { display: flex; align-items: center; gap: 8px; }

.low-list { display: flex; flex-direction: column; gap: 10px; }

.low-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
}

.low-title-text {
  font-weight: 500;
  color: #1e293b;
  font-size: 14px;
  margin-bottom: 4px;
}

.low-meta {
  display: flex;
  gap: 10px;
  align-items: center;
  font-size: 12px;
}

.low-time { color: #dc2626; }

@media (max-width: 900px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
  .content-grid { grid-template-columns: 1fr; }
}
</style>
