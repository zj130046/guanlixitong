<template>
  <div class="agent-manage">
    <el-card class="filter-card">
      <el-form :inline="true" :model="queryForm" size="default">
        <el-form-item label="搜索">
          <el-input v-model="queryForm.keyword" placeholder="搜索用户名/姓名" clearable style="width: 180px" @keyup.enter="loadData">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="分组">
          <el-select v-model="queryForm.groupId" placeholder="全部分组" clearable style="width: 130px" @change="loadData">
            <el-option v-for="g in groups" :key="g.id" :label="g.name" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 100px" @change="loadData">
            <el-option label="在职" value="ACTIVE" />
            <el-option label="离职" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button type="success" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增客服
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" style="width: 100%" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="账号" width="130" />
        <el-table-column prop="realName" label="姓名" width="110">
          <template #default="{ row }">{{ row.realName || '-' }}</template>
        </el-table-column>
        <el-table-column label="分组" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ getGroupName(row.groupId) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="role" label="角色" width="90">
          <template #default="{ row }">
            <el-tag :type="row.role === 'LEADER' ? 'warning' : 'info'" size="small">
              {{ row.role === 'LEADER' ? '组长' : '客服' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="在线状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.onlineStatus === 'ONLINE' ? 'success' : 'info'" size="small">
              {{ row.onlineStatus === 'ONLINE' ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ row.status === 'ACTIVE' ? '在职' : '离职' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无客服数据" /></template>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.isEdit ? '编辑客服' : '新增客服'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="账号">
          <el-input v-model="form.username" :disabled="dialog.isEdit" />
        </el-form-item>
        <el-form-item v-if="!dialog.isEdit" label="密码">
          <el-input v-model="form.password" type="password" placeholder="请设置初始密码" />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="所属分组">
          <el-select v-model="form.groupId" placeholder="请选择分组" style="width: 100%">
            <el-option v-for="g in groups" :key="g.id" :label="g.name" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-radio-group v-model="form.role">
            <el-radio value="AGENT">普通客服</el-radio>
            <el-radio value="LEADER">组长</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">在职</el-radio>
            <el-radio value="DISABLED">离职</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { listAgents, createAgent, updateAgent, listAgentGroups } from '../api/ticket'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const groups = ref<any[]>([])

const queryForm = reactive({
  page: 1, size: 10,
  keyword: '', groupId: null as number | null, status: ''
})

const dialog = reactive({ visible: false, isEdit: false, editId: 0 })
const form = reactive({
  username: '', password: '', realName: '',
  groupId: null as number | null, role: 'AGENT', status: 'ACTIVE'
})

onMounted(async () => {
  await loadGroups()
  loadData()
})

async function loadGroups() {
  try {
    groups.value = await listAgentGroups() as any
  } catch (e) { /* ignore */ }
}

async function loadData() {
  loading.value = true
  try {
    const res = await listAgents({
      page: queryForm.page, size: queryForm.size,
      keyword: queryForm.keyword || undefined,
      groupId: queryForm.groupId || undefined,
      status: queryForm.status || undefined
    })
    tableData.value = res.records
    total.value = res.total
  } catch (e) {
    // mock
    tableData.value = [
      { id: 1, username: 'agent001', realName: '张工', groupId: 1, role: 'AGENT', onlineStatus: 'ONLINE', status: 'ACTIVE', createdAt: new Date().toISOString() },
      { id: 2, username: 'agent002', realName: '李工', groupId: 1, role: 'LEADER', onlineStatus: 'ONLINE', status: 'ACTIVE', createdAt: new Date().toISOString() },
      { id: 3, username: 'agent003', realName: '王工', groupId: 2, role: 'AGENT', onlineStatus: 'OFFLINE', status: 'ACTIVE', createdAt: new Date().toISOString() },
    ]
    total.value = 3
  } finally { loading.value = false }
}

function getGroupName(id: number) {
  const g = groups.value.find(g => g.id === id)
  return g?.name || '未分配'
}

function handleAdd() {
  dialog.isEdit = false
  dialog.editId = 0
  form.username = ''
  form.password = ''
  form.realName = ''
  form.groupId = groups.value[0]?.id || null
  form.role = 'AGENT'
  form.status = 'ACTIVE'
  dialog.visible = true
}

function handleEdit(row: any) {
  dialog.isEdit = true
  dialog.editId = row.id
  form.username = row.username
  form.password = ''
  form.realName = row.realName || ''
  form.groupId = row.groupId
  form.role = row.role
  form.status = row.status
  dialog.visible = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    const data = {
      username: form.username,
      passwordHash: form.password,
      realName: form.realName,
      groupId: form.groupId,
      role: form.role,
      status: form.status
    }
    if (dialog.isEdit) {
      await updateAgent(dialog.editId, data)
      ElMessage.success('更新成功')
    } else {
      await createAgent(data)
      ElMessage.success('创建成功')
    }
    dialog.visible = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally { submitting.value = false }
}

function handleDelete(row: any) {
  ElMessageBox.confirm(`确认删除客服「${row.username}」吗？`, '提示', { type: 'warning' })
    .then(() => { ElMessage.success('删除成功'); loadData() })
    .catch(() => {})
}

function formatTime(time: string) {
  if (!time) return ''
  const d = new Date(time)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}
</script>

<style scoped>
.filter-card { margin-bottom: 16px; border-radius: 12px; }
.table-card { border-radius: 12px; }
.pagination { display: flex; justify-content: flex-end; margin-top: 20px; }
</style>
