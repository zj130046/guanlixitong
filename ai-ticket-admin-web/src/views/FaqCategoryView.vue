<template>
  <div class="faq-category">
    <el-card class="page-card">
      <template #header>
        <div class="card-header">
          <span>FAQ 分类管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增分类
          </el-button>
        </div>
      </template>

      <el-table :data="categories" v-loading="loading" row-key="id" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="分类名称" min-width="200" />
        <el-table-column prop="sortOrder" label="排序" width="100" />
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无分类" />
        </template>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.isEdit ? '编辑分类' : '新增分类'" width="480px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
          <span style="margin-left: 10px; color: #94a3b8; font-size: 12px">数字越小越靠前</span>
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
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  listFaqCategories, createFaqCategory, updateFaqCategory, deleteFaqCategory
} from '../api/ticket'

const loading = ref(false)
const categories = ref<any[]>([])
const submitting = ref(false)
const formRef = ref<FormInstance>()

const dialog = reactive({
  visible: false,
  isEdit: false,
  editId: 0
})

const form = reactive({
  name: '',
  sortOrder: 0
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    categories.value = await listFaqCategories() as any
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

function handleAdd() {
  dialog.isEdit = false
  dialog.editId = 0
  form.name = ''
  form.sortOrder = 0
  dialog.visible = true
}

function handleEdit(row: any) {
  dialog.isEdit = true
  dialog.editId = row.id
  form.name = row.name
  form.sortOrder = row.sortOrder || 0
  dialog.visible = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (dialog.isEdit) {
        await updateFaqCategory(dialog.editId, { name: form.name, sortOrder: form.sortOrder })
        ElMessage.success('更新成功')
      } else {
        await createFaqCategory({ name: form.name, sortOrder: form.sortOrder })
        ElMessage.success('创建成功')
      }
      dialog.visible = false
      loadData()
    } catch (e: any) {
      ElMessage.error(e.message || '操作失败')
    } finally {
      submitting.value = false
    }
  })
}

function handleDelete(row: any) {
  ElMessageBox.confirm(`确认删除分类「${row.name}」吗？`, '提示', {
    type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消'
  }).then(async () => {
    try {
      await deleteFaqCategory(row.id)
      ElMessage.success('删除成功')
      loadData()
    } catch (e: any) {
      ElMessage.error(e.message || '删除失败')
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
.page-card { border-radius: 12px; }
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #1e293b;
}
</style>
