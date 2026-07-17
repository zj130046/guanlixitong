<template>
  <div class="pool-page">
    <el-card class="filter-card">
      <el-form :inline="true" :model="queryForm" size="default">
        <el-form-item label="搜索">
          <el-input
            v-model="queryForm.keyword"
            placeholder="搜索工单标题"
            clearable
            style="width: 200px"
            @keyup.enter="loadData"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="queryForm.category" placeholder="全部分类" clearable style="width: 140px" @change="loadData">
            <el-option label="校园报修" value="校园报修" />
            <el-option label="产品售后" value="产品售后" />
            <el-option label="投诉建议" value="投诉建议" />
            <el-option label="业务咨询" value="业务咨询" />
            <el-option label="AI 咨询" value="AI 咨询" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="queryForm.priority" placeholder="全部优先级" clearable style="width: 120px" @change="loadData">
            <el-option label="低" value="LOW" />
            <el-option label="普通" value="NORMAL" />
            <el-option label="高" value="HIGH" />
            <el-option label="紧急" value="URGENT" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleRefresh">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>公共工单池 <el-tag size="small" type="warning">{{ total }} 条待接</el-tag></span>
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
        <el-table-column prop="priority" label="优先级" width="90">
          <template #default="{ row }">
            <el-tag :type="priorityColor[row.priority]" size="small">
              {{ priorityLabel[row.priority] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requesterName" label="提交人" width="100" />
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleAccept(row.id)">
              <el-icon><Checked /></el-icon>
              接单
            </el-button>
            <el-button size="small" @click="goDetail(row.id)">
              <el-icon><View /></el-icon>
              详情
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="工单池暂无工单" />
        </template>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="queryForm.page"
          v-model:page-size="queryForm.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 接单确认弹窗 -->
    <el-dialog v-model="acceptDialogVisible" title="确认接单" width="420px">
      <p>确认要接管该工单吗？接单后工单将分配到您的名下。</p>
      <template #footer>
        <el-button @click="acceptDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="acceptLoading" @click="confirmAccept">
          确认接单
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Checked, View } from '@element-plus/icons-vue'
import {
  ticketPool, acceptTicket,
  priorityColor, priorityLabel
} from '../api/ticket'
import type { Ticket } from '../api/ticket'

const router = useRouter()
const loading = ref(false)
const tableData = ref<Ticket[]>([])
const total = ref(0)

const queryForm = reactive({
  page: 1,
  size: 10,
  keyword: '',
  category: '',
  priority: ''
})

const acceptDialogVisible = ref(false)
const acceptLoading = ref(false)
const acceptId = ref<number | null>(null)

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const res = await ticketPool({
      page: queryForm.page,
      size: queryForm.size,
      category: queryForm.category || undefined,
      priority: queryForm.priority || undefined
    })
    tableData.value = res.records
    total.value = res.total
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}

function handleRefresh() {
  loadData()
  ElMessage.success('已刷新')
}

function handleAccept(id: number) {
  acceptId.value = id
  acceptDialogVisible.value = true
}

async function confirmAccept() {
  if (!acceptId.value) return
  acceptLoading.value = true
  try {
    await acceptTicket(acceptId.value)
    ElMessage.success('接单成功！')
    acceptDialogVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '接单失败')
  } finally {
    acceptLoading.value = false
  }
}

function goDetail(id: number) {
  router.push(`/tickets/${id}`)
}

function formatTime(time: string) {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped>
.filter-card { margin-bottom: 16px; border-radius: 12px; }
.table-card { border-radius: 12px; }
.card-header { font-weight: 600; color: #1e293b; }
.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
