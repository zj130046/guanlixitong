<template>
  <div class="faq-entry">
    <el-card class="filter-card">
      <el-form :inline="true" :model="queryForm" size="default">
        <el-form-item label="搜索">
          <el-input
            v-model="queryForm.keyword"
            placeholder="搜索问题/答案"
            clearable
            style="width: 200px"
            @keyup.enter="loadData"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="queryForm.categoryId" placeholder="全部分类" clearable style="width: 150px" @change="loadData">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button type="success" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增
          </el-button>
          <el-button type="warning" @click="handleRebuildVectors">
            <el-icon><Refresh /></el-icon>
            重建向量
          </el-button>
          <el-button @click="handleExportCsv">
            <el-icon><Download /></el-icon>
            导出 CSV
          </el-button>
          <el-button @click="triggerImport">
            <el-icon><Upload /></el-icon>
            导入 CSV
          </el-button>
          <input ref="importInput" type="file" accept=".csv" style="display:none" @change="handleImportCsv" />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>FAQ 问答列表 <el-tag size="small" type="info">共 {{ total }} 条</el-tag></span>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" style="width: 100%" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="question" label="问题" min-width="250" show-overflow-tooltip />
        <el-table-column prop="answer" label="答案" min-width="300" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="answer-text">{{ row.answer }}</span>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ getCategoryName(row.categoryId) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-switch
              :model-value="row.enabled === 1"
              size="small"
              @change="(val: boolean) => toggleEnabled(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="150">
          <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无FAQ数据" />
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialog.visible" :title="dialog.isEdit ? '编辑FAQ' : '新增FAQ'" width="640px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="所属分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题" prop="question">
          <el-input v-model="form.question" type="textarea" :rows="2" placeholder="请输入问题" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="答案" prop="answer">
          <el-input v-model="form.answer" type="textarea" :rows="5" placeholder="请输入答案" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="form.keywords" placeholder="多个关键词用逗号分隔" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="禁用" />
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
import { Search, Plus, Refresh, Download, Upload } from '@element-plus/icons-vue'
import { TOKEN_KEY } from '../api/client'
import {
  listFaqCategories, listFaqEntries, createFaqEntry, updateFaqEntry, deleteFaqEntry,
  rebuildFaqVectors, importFaqEntries
} from '../api/ticket'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const categories = ref<any[]>([])
const formRef = ref<FormInstance>()
const importInput = ref<HTMLInputElement>()

const queryForm = reactive({
  page: 1,
  size: 10,
  keyword: '',
  categoryId: null as number | null
})

const dialog = reactive({
  visible: false,
  isEdit: false,
  editId: 0
})

const form = reactive({
  question: '',
  answer: '',
  categoryId: null as number | null,
  keywords: '',
  enabled: true
})

const rules: FormRules = {
  question: [{ required: true, message: '请输入问题', trigger: 'blur' }],
  answer: [{ required: true, message: '请输入答案', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

onMounted(async () => {
  await loadCategories()
  loadData()
})

async function loadCategories() {
  try {
    categories.value = await listFaqCategories() as any
  } catch (e) { /* ignore */ }
}

async function loadData() {
  loading.value = true
  try {
    const res = await listFaqEntries({
      page: queryForm.page,
      size: queryForm.size,
      keyword: queryForm.keyword || undefined,
      categoryId: queryForm.categoryId || undefined
    })
    tableData.value = res.records
    total.value = res.total
  } catch (e) { /* ignore */ }
  finally { loading.value = false }
}

function getCategoryName(id: number) {
  const cat = categories.value.find(c => c.id === id)
  return cat?.name || '未分类'
}

function handleAdd() {
  dialog.isEdit = false
  dialog.editId = 0
  form.question = ''
  form.answer = ''
  form.categoryId = categories.value[0]?.id || null
  form.keywords = ''
  form.enabled = true
  dialog.visible = true
}

function handleEdit(row: any) {
  dialog.isEdit = true
  dialog.editId = row.id
  form.question = row.question
  form.answer = row.answer
  form.categoryId = row.categoryId
  form.keywords = row.keywords || ''
  form.enabled = row.enabled === 1
  dialog.visible = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const data = {
        question: form.question,
        answer: form.answer,
        categoryId: form.categoryId,
        keywords: form.keywords,
        enabled: form.enabled ? 1 : 0
      }
      if (dialog.isEdit) {
        await updateFaqEntry(dialog.editId, data)
        ElMessage.success('更新成功')
      } else {
        await createFaqEntry(data)
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
  ElMessageBox.confirm(`确认删除该 FAQ 吗？`, '提示', {
    type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消'
  }).then(async () => {
    try {
      await deleteFaqEntry(row.id)
      ElMessage.success('删除成功')
      loadData()
    } catch (e: any) {
      ElMessage.error(e.message || '删除失败')
    }
  }).catch(() => {})
}

async function toggleEnabled(row: any, val: boolean) {
  try {
    await updateFaqEntry(row.id, { enabled: val ? 1 : 0 })
    ElMessage.success(val ? '已启用' : '已禁用')
  } catch (e: any) {
    row.enabled = val ? 0 : 1
    ElMessage.error(e.message || '操作失败')
  }
}

function triggerImport() {
  importInput.value?.click()
}

async function handleImportCsv(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  const fr = new FileReader()
  fr.onload = async (evt) => {
    try {
      const csv = (evt.target as FileReader).result as string
      const res = await importFaqEntries(csv) as any
      ElMessage.success(`导入完成：成功 ${res.imported} 条，跳过 ${res.skipped} 条`)
      loadData()
    } catch (err: any) {
      ElMessage.error(err.message || '导入失败')
    }
  }
  fr.readAsText(file)
  target.value = ''
}

async function handleExportCsv() {
  try {
    const token = localStorage.getItem(TOKEN_KEY)
    const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
    const res = await fetch(`${baseURL}/admin/faq/entries/export`, {
      headers: { Authorization: token || '' }
    })
    if (!res.ok) throw new Error('导出失败')
    const blob = await res.blob()
    const disposition = res.headers.get('content-disposition') || ''
    const match = disposition.match(/filename\*?=(?:UTF-8'')?([^;]+)/)
    const filename = match ? decodeURIComponent(match[1]) : `faq-${Date.now()}.csv`
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = filename
    a.click()
    URL.revokeObjectURL(a.href)
    ElMessage.success('导出成功')
  } catch (err: any) {
    ElMessage.error(err.message || '导出失败')
  }
}

async function handleRebuildVectors() {
  ElMessageBox.confirm('确认重建所有 FAQ 的向量索引吗？这可能需要一些时间。', '提示', { type: 'info' })
    .then(async () => {
      try {
        const res = await rebuildFaqVectors() as any
        ElMessage.success(`向量重建完成，共更新 ${res.rebuilt || 0} 条`)
        loadData()
      } catch (e: any) {
        ElMessage.error(e.message || '重建失败')
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
.card-header { font-weight: 600; color: #1e293b; }
.answer-text { color: #64748b; }
.pagination { display: flex; justify-content: flex-end; margin-top: 20px; }
</style>
