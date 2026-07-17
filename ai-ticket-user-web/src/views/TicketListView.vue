<template>
  <div class="list-page">
    <el-card class="filter-card">
      <el-form :inline="true" :model="queryForm" size="default">
        <el-form-item label="搜索">
          <el-input
            v-model="queryForm.keyword"
            placeholder="搜索工单标题/描述"
            clearable
            style="width: 220px"
            @keyup.enter="loadData"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部状态" clearable style="width: 140px" @change="loadData">
            <el-option label="待接单" value="ASSIGNED" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="跟进中" value="FOLLOWING" />
            <el-option label="已完结" value="COMPLETED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="table-header">
          <span class="table-title">我的工单</span>
          <el-button type="primary" @click="goCreate">
            <el-icon><Plus /></el-icon>
            创建工单
          </el-button>
        </div>
      </template>

      <el-table
        :data="tableData"
        v-loading="loading"
        style="width: 100%"
        @row-click="goDetail"
        :row-class-name="() => 'clickable-row'"
      >
        <el-table-column prop="id" label="工单号" width="100">
          <template #default="{ row }">
            <span class="ticket-id">#{{ row.id }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200">
          <template #default="{ row }">
            <div class="ticket-title">{{ row.title }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.category || '未分类' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="100">
          <template #default="{ row }">
            <el-tag :type="priorityColor[row.priority]" size="small">
              {{ priorityLabel[row.priority] || row.priority }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusColor[row.status]" size="small">
              {{ statusLabel[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assignee" label="处理人" width="120">
          <template #default="{ row }">
            <span class="assignee">{{ row.assignee || '待分配' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click.stop="goDetail(row)">详情</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无工单数据">
            <el-button type="primary" @click="goCreate">创建工单</el-button>
          </el-empty>
        </template>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="queryForm.page"
          v-model:page-size="queryForm.size"
          :total="total"
          :page-sizes="[5, 10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Plus } from '@element-plus/icons-vue'
import { listUserTickets, statusColor, statusLabel, priorityColor, priorityLabel } from '../api/ticket'
import type { Ticket } from '../api/ticket'

const router = useRouter()
const loading = ref(false)
const tableData = ref<Ticket[]>([])
const total = ref(0)

const queryForm = reactive({
  page: 1,
  size: 10,
  keyword: '',
  status: ''
})

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const res = await listUserTickets({
      page: queryForm.page,
      size: queryForm.size,
      keyword: queryForm.keyword || undefined,
      status: queryForm.status || undefined
    })
    tableData.value = res.records
    total.value = res.total
  } catch (e) {
    // 错误由拦截器提示
  } finally {
    loading.value = false
  }
}

function handleReset() {
  queryForm.keyword = ''
  queryForm.status = ''
  queryForm.page = 1
  loadData()
}

function goCreate() {
  router.push('/tickets/create')
}

function goDetail(row: Ticket) {
  router.push(`/tickets/${row.id}`)
}

function formatTime(time: string) {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped>
.filter-card {
  margin-bottom: 16px;
  border-radius: 12px;
}

.table-card {
  border-radius: 12px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.table-title { font-weight: 600; color: #1e293b; }

.ticket-id { color: #64748b; font-family: monospace; font-size: 13px; }
.ticket-title {
  font-weight: 500;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 300px;
}
.assignee { color: #475569; font-size: 13px; }

:deep(.clickable-row) {
  cursor: pointer;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
