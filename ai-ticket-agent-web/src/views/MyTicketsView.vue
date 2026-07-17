<template>
  <div class="my-tickets-page">
    <el-card class="filter-card">
      <el-form :inline="true" :model="queryForm" size="default">
        <el-form-item label="搜索">
          <el-input
            v-model="queryForm.keyword"
            placeholder="搜索工单标题/描述"
            clearable
            style="width: 200px"
            @keyup.enter="loadData"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部状态" clearable style="width: 130px" @change="loadData">
            <el-option label="已接单" value="ACCEPTED" />
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
        <div class="card-header">
          <span>我的工单 <el-tag size="small" type="primary">{{ total }} 条</el-tag></span>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" style="width: 100%" stripe>
        <el-table-column prop="id" label="工单号" width="90">
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
        <el-table-column prop="createdAt" label="创建时间" width="150">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="goDetail(row.id)">查看</el-button>
            <el-dropdown
              v-if="canOperate(row.status)"
              trigger="click"
              @command="(cmd: string) => handleAction(row.id, cmd)"
            >
              <el-button type="primary" size="small" plain>处理 <el-icon><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="process">开始处理</el-dropdown-item>
                  <el-dropdown-item command="follow">标记跟进</el-dropdown-item>
                  <el-dropdown-item command="complete">完结工单</el-dropdown-item>
                  <el-dropdown-item command="reject" divided>驳回工单</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无工单" />
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

    <!-- 操作确认弹窗 -->
    <el-dialog v-model="actionDialog.visible" :title="actionDialog.title" width="460px">
      <el-form :model="actionForm" label-width="80px">
        <el-form-item label="备注">
          <el-input
            v-model="actionForm.remark"
            type="textarea"
            :rows="3"
            :placeholder="actionDialog.placeholder"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="actionDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="confirmAction">
          确认
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, ArrowDown } from '@element-plus/icons-vue'
import {
  myTickets, processTicket, followTicket, completeTicket, rejectTicket,
  statusColor, statusLabel, priorityColor, priorityLabel
} from '../api/ticket'
import type { Ticket } from '../api/ticket'

const router = useRouter()
const loading = ref(false)
const tableData = ref<Ticket[]>([])
const total = ref(0)
const actionLoading = ref(false)

const queryForm = reactive({
  page: 1,
  size: 10,
  keyword: '',
  status: ''
})

const actionDialog = reactive({
  visible: false,
  title: '',
  action: '',
  ticketId: 0,
  placeholder: ''
})
const actionForm = reactive({ remark: '' })

const actionTitles: Record<string, string> = {
  process: '开始处理',
  follow: '标记跟进',
  complete: '完结工单',
  reject: '驳回工单'
}
const actionPlaceholders: Record<string, string> = {
  process: '请输入处理说明（选填）',
  follow: '请输入跟进内容（选填）',
  complete: '请输入处理结果说明（选填）',
  reject: '请输入驳回原因（必填）'
}

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const res = await myTickets({
      page: queryForm.page,
      size: queryForm.size,
      keyword: queryForm.keyword || undefined,
      status: queryForm.status || undefined
    })
    tableData.value = res.records
    total.value = res.total
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

function handleReset() {
  queryForm.keyword = ''
  queryForm.status = ''
  queryForm.page = 1
  loadData()
}

function canOperate(status: string) {
  return !['COMPLETED', 'REJECTED', 'ARCHIVED'].includes(status)
}

function handleAction(id: number, action: string) {
  actionDialog.ticketId = id
  actionDialog.action = action
  actionDialog.title = actionTitles[action] || '操作'
  actionDialog.placeholder = actionPlaceholders[action] || ''
  actionForm.remark = ''
  actionDialog.visible = true
}

async function confirmAction() {
  const { action, ticketId } = actionDialog
  if (action === 'reject' && !actionForm.remark.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }

  actionLoading.value = true
  try {
    let res
    switch (action) {
      case 'process': res = await processTicket(ticketId, actionForm.remark); break
      case 'follow': res = await followTicket(ticketId, actionForm.remark); break
      case 'complete': res = await completeTicket(ticketId, actionForm.remark); break
      case 'reject': res = await rejectTicket(ticketId, actionForm.remark); break
    }
    ElMessage.success('操作成功')
    actionDialog.visible = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    actionLoading.value = false
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
.pagination { display: flex; justify-content: flex-end; margin-top: 20px; }
</style>
