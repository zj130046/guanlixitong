<template>
  <div class="ticket-manage">
    <el-card class="filter-card">
      <el-form :inline="true" :model="queryForm" size="default">
        <el-form-item label="搜索">
          <el-input
            v-model="queryForm.keyword"
            placeholder="搜索标题/描述"
            clearable
            style="width: 200px"
            @keyup.enter="loadData"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 120px" @change="loadData">
            <el-option v-for="(label, key) in statusLabel" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="queryForm.category" placeholder="全部分类" clearable style="width: 120px" @change="loadData">
            <el-option label="校园报修" value="校园报修" />
            <el-option label="产品售后" value="产品售后" />
            <el-option label="投诉建议" value="投诉建议" />
            <el-option label="业务咨询" value="业务咨询" />
            <el-option label="AI 咨询" value="AI 咨询" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="queryForm.priority" placeholder="全部" clearable style="width: 100px" @change="loadData">
            <el-option v-for="(label, key) in priorityLabel" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleExport">
            <el-icon><Download /></el-icon>
            导出
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>工单列表 <el-tag size="small" type="info">共 {{ total }} 条</el-tag></span>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" style="width: 100%" stripe>
        <el-table-column prop="id" label="工单号" width="80">
          <template #default="{ row }">#{{ row.id }}</template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
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
        <el-table-column prop="requesterName" label="提交人" width="90" />
        <el-table-column prop="assignee" label="处理人" width="90">
          <template #default="{ row }">{{ row.assignee || '待分配' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="150">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="goDetail(row.id)">详情</el-button>
            <el-dropdown trigger="click" @command="(cmd: string) => handleAction(row, cmd)">
              <el-button size="small" link>
                更多操作 <el-icon><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="priority">调整优先级</el-dropdown-item>
                  <el-dropdown-item command="assign">改派工单</el-dropdown-item>
                  <el-dropdown-item command="archive" divided>归档工单</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无工单数据" />
        </template>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="queryForm.page"
          v-model:page-size="queryForm.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 调整优先级弹窗 -->
    <el-dialog v-model="priorityDialog.visible" title="调整优先级" width="400px">
      <el-form label-width="80px">
        <el-form-item label="优先级">
          <el-radio-group v-model="priorityDialog.priority">
            <el-radio value="LOW">低</el-radio>
            <el-radio value="NORMAL">普通</el-radio>
            <el-radio value="HIGH">高</el-radio>
            <el-radio value="URGENT">紧急</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="priorityDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="confirmPriority">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Download, ArrowDown } from '@element-plus/icons-vue'
import {
  adminTickets, updateAdminTicket,
  statusColor, statusLabel, priorityColor, priorityLabel
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
  status: '',
  category: '',
  priority: ''
})

const priorityDialog = reactive({
  visible: false,
  ticketId: 0,
  priority: 'NORMAL'
})

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const res = await adminTickets({
      page: queryForm.page,
      size: queryForm.size,
      keyword: queryForm.keyword || undefined,
      status: queryForm.status || undefined,
      category: queryForm.category || undefined,
      priority: queryForm.priority || undefined
    })
    tableData.value = res.records
    total.value = res.total
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

function handleReset() {
  queryForm.keyword = ''
  queryForm.status = ''
  queryForm.category = ''
  queryForm.priority = ''
  queryForm.page = 1
  loadData()
}

function handleExport() {
  ElMessage.info('导出功能开发中...')
}

function goDetail(id: number) {
  router.push(`/tickets/${id}`)
}

function handleAction(row: Ticket, cmd: string) {
  if (cmd === 'priority') {
    priorityDialog.ticketId = row.id
    priorityDialog.priority = row.priority
    priorityDialog.visible = true
  } else if (cmd === 'assign') {
    ElMessage.info('改派功能开发中...')
  } else if (cmd === 'archive') {
    ElMessageBox.confirm('确认归档该工单吗？', '提示', { type: 'warning' })
      .then(() => {
        ElMessage.success('归档成功')
      }).catch(() => {})
  }
}

async function confirmPriority() {
  try {
    await updateAdminTicket(priorityDialog.ticketId, { priority: priorityDialog.priority })
    ElMessage.success('优先级已调整')
    priorityDialog.visible = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
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
.pagination { display: flex; justify-content: flex-end; margin-top: 20px; }
</style>
