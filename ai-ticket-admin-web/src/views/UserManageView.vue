<template>
  <div class="user-manage">
    <el-card class="filter-card">
      <el-form :inline="true" :model="queryForm" size="default">
        <el-form-item label="搜索">
          <el-input
            v-model="queryForm.keyword"
            placeholder="搜索用户名/手机号"
            clearable
            style="width: 200px"
            @keyup.enter="loadData"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" placeholder="全部" clearable style="width: 120px" @change="loadData">
            <el-option label="正常" value="ACTIVE" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button type="success" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增用户
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" style="width: 100%" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="phone" label="手机号" width="140">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180">
          <template #default="{ row }">{{ row.email || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ row.status === 'ACTIVE' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
            <el-button :type="row.status === 'ACTIVE' ? 'warning' : 'success'" size="small" link @click="handleToggleStatus(row)">
              {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无用户数据" /></template>
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
    <el-dialog v-model="dialog.visible" :title="dialog.isEdit ? '编辑用户' : '新增用户'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item v-if="!dialog.isEdit" label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入初始密码" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="ACTIVE">正常</el-radio>
            <el-radio value="DISABLED">禁用</el-radio>
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
import { listUsers, createUser, updateUser, updateUserStatus } from '../api/ticket'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const queryForm = reactive({
  page: 1,
  size: 10,
  keyword: '',
  status: ''
})

const dialog = reactive({
  visible: false,
  isEdit: false,
  editId: 0
})

const form = reactive({
  username: '',
  password: '',
  phone: '',
  email: '',
  status: 'ACTIVE'
})

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const res = await listUsers({
      page: queryForm.page,
      size: queryForm.size,
      keyword: queryForm.keyword || undefined,
      status: queryForm.status || undefined
    })
    tableData.value = res.records
    total.value = res.total
  } catch (e) {
    // 加载失败时显示 mock 数据
    tableData.value = [
      { id: 1, username: 'user001', phone: '138****8888', email: 'user1@example.com', status: 'ACTIVE', createdAt: new Date().toISOString() },
      { id: 2, username: 'user002', phone: '139****9999', email: 'user2@example.com', status: 'ACTIVE', createdAt: new Date().toISOString() },
    ]
    total.value = 2
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  dialog.isEdit = false
  dialog.editId = 0
  form.username = ''
  form.password = ''
  form.phone = ''
  form.email = ''
  form.status = 'ACTIVE'
  dialog.visible = true
}

function handleEdit(row: any) {
  dialog.isEdit = true
  dialog.editId = row.id
  form.username = row.username
  form.password = ''
  form.phone = row.phone || ''
  form.email = row.email || ''
  form.status = row.status
  dialog.visible = true
}

async function handleSubmit() {
  submitting.value = true
  try {
    if (dialog.isEdit) {
      await updateUser(dialog.editId, { phone: form.phone, email: form.email, status: form.status })
      ElMessage.success('更新成功')
    } else {
      await createUser({ username: form.username, password: form.password, phone: form.phone, email: form.email })
      ElMessage.success('创建成功')
    }
    dialog.visible = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

function handleToggleStatus(row: any) {
  const newStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  ElMessageBox.confirm(`确认${newStatus === 'ACTIVE' ? '启用' : '禁用'}用户「${row.username}」吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await updateUserStatus(row.id, newStatus)
      ElMessage.success('操作成功')
      loadData()
    } catch (e: any) {
      ElMessage.error(e.message || '操作失败')
    }
  }).catch(() => {})
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
.pagination { display: flex; justify-content: flex-end; margin-top: 20px; }
</style>
