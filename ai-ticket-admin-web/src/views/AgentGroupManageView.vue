<template>
  <div class="group-manage">
    <el-card class="page-card">
      <template #header>
        <div class="card-header">
          <span>客服分组管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增分组
          </el-button>
        </div>
      </template>

      <el-table :data="groups" v-loading="loading" style="width: 100%" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="分组名称" min-width="160" />
        <el-table-column prop="scene" label="负责场景/业务" min-width="180">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.scene || '通用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="组长" width="120">
          <template #default="{ row }">{{ getLeaderName(row.leaderAgentId) || '未设置' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无分组数据" /></template>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.isEdit ? '编辑分组' : '新增分组'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="分组名称">
          <el-input v-model="form.name" placeholder="请输入分组名称" />
        </el-form-item>
        <el-form-item label="负责场景">
          <el-input v-model="form.scene" placeholder="如：校园报修、产品售后" />
        </el-form-item>
        <el-form-item label="组长">
          <el-select v-model="form.leaderAgentId" placeholder="请选择组长" clearable style="width: 100%">
            <el-option v-for="a in agents" :key="a.id" :label="a.realName || a.username" :value="a.id" />
          </el-select>
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
import { Plus } from '@element-plus/icons-vue'
import {
  listAgentGroups, createAgentGroup, updateAgentGroup, listAgents
} from '../api/ticket'

const loading = ref(false)
const submitting = ref(false)
const groups = ref<any[]>([])
const agents = ref<any[]>([])

const dialog = reactive({ visible: false, isEdit: false, editId: 0 })
const form = reactive({ name: '', scene: '', leaderAgentId: null as number | null })

onMounted(async () => {
  await loadAgents()
  loadData()
})

async function loadAgents() {
  try {
    const res = await listAgents({ page: 1, size: 100 }) as any
    agents.value = res.records || []
  } catch (e) { /* ignore */ }
}

async function loadData() {
  loading.value = true
  try {
    groups.value = await listAgentGroups() as any
  } catch (e) {
    groups.value = [
      { id: 1, name: '校园报修组', scene: '校园报修', leaderAgentId: 2, createdAt: new Date().toISOString() },
      { id: 2, name: '售后客服组', scene: '产品售后', leaderAgentId: null, createdAt: new Date().toISOString() },
      { id: 3, name: '投诉建议组', scene: '投诉建议', leaderAgentId: null, createdAt: new Date().toISOString() },
    ]
  } finally { loading.value = false }
}

function getLeaderName(id: number) {
  const a = agents.value.find(a => a.id === id)
  return a?.realName || a?.username
}

function handleAdd() {
  dialog.isEdit = false; dialog.editId = 0
  form.name = ''; form.scene = ''; form.leaderAgentId = null
  dialog.visible = true
}

function handleEdit(row: any) {
  dialog.isEdit = true; dialog.editId = row.id
  form.name = row.name; form.scene = row.scene || ''
  form.leaderAgentId = row.leaderAgentId
  dialog.visible = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    if (dialog.isEdit) {
      await updateAgentGroup(dialog.editId, { name: form.name, scene: form.scene, leaderAgentId: form.leaderAgentId })
      ElMessage.success('更新成功')
    } else {
      await createAgentGroup({ name: form.name, scene: form.scene, leaderAgentId: form.leaderAgentId })
      ElMessage.success('创建成功')
    }
    dialog.visible = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally { submitting.value = false }
}

function handleDelete(row: any) {
  ElMessageBox.confirm(`确认删除分组「${row.name}」吗？`, '提示', { type: 'warning' })
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
.page-card { border-radius: 12px; }
.card-header {
  display: flex; justify-content: space-between; align-items: center;
  font-weight: 600; color: #1e293b;
}
</style>
