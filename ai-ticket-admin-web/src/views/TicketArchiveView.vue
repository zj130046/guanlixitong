<template>
  <div class="ticket-archive">
    <el-card class="filter-card">
      <el-form :inline="true" :model="queryForm" size="default">
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="queryForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 280px"
          />
        </el-form-item>
        <el-form-item label="搜索">
          <el-input v-model="queryForm.keyword" placeholder="搜索工单号/标题" clearable style="width: 200px">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="queryForm.category" placeholder="全部分类" clearable style="width: 120px">
            <el-option label="校园报修" value="校园报修" />
            <el-option label="产品售后" value="产品售后" />
            <el-option label="投诉建议" value="投诉建议" />
            <el-option label="业务咨询" value="业务咨询" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button type="success" @click="handleExport">
            <el-icon><Download /></el-icon>导出
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>归档工单列表 <el-tag size="small" type="info">共 {{ total }} 条</el-tag></span>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" style="width: 100%" stripe>
        <el-table-column prop="id" label="工单号" width="90">
          <template #default="{ row }">#{{ row.id }}</template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="110">
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
        <el-table-column prop="requesterName" label="提交人" width="100" />
        <el-table-column prop="assignee" label="处理人" width="90" />
        <el-table-column prop="completedAt" label="完结时间" width="170">
          <template #default="{ row }">{{ row.completedAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="goDetail(row.id)">查看详情</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无归档工单" /></template>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="queryForm.page"
          v-model:page-size="queryForm.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Download } from '@element-plus/icons-vue'
import { adminTickets, priorityColor, priorityLabel } from '../api/ticket'

const router = useRouter()

const loading = ref(false)
const total = ref(0)
const tableData = ref<any[]>([])

const queryForm = reactive({
  page: 1, size: 10,
  keyword: '', category: '',
  dateRange: [] as string[]
})

onMounted(loadData)
watch(() => queryForm.page, loadData)
watch(() => queryForm.size, loadData)

async function loadData() {
  loading.value = true
  try {
    const res = await adminTickets({
      page: queryForm.page,
      size: queryForm.size,
      status: 'ARCHIVED',
      category: queryForm.category || undefined,
      keyword: queryForm.keyword || undefined
    })
    tableData.value = res.records || []
    total.value = res.total || 0
  } catch (e: any) {
    ElMessage.error(e.message || '加载归档工单失败')
  } finally {
    loading.value = false
  }
}

function handleExport() {
  ElMessage.success('归档工单导出中，请稍候...')
}

function goDetail(id: number) {
  router.push(`/tickets/${id}`)
}
</script>

<style scoped>
.filter-card { margin-bottom: 16px; border-radius: 12px; }
.table-card { border-radius: 12px; }
.card-header { font-weight: 600; color: #1e293b; }
.pagination { display: flex; justify-content: flex-end; margin-top: 20px; }
</style>
