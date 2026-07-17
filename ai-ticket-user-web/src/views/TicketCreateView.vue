<template>
  <div class="create-page">
    <el-card class="form-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">创建工单</span>
          <span class="card-subtitle">填写以下信息，提交后客服人员会尽快处理您的问题</span>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        label-position="right"
        size="default"
      >
        <el-form-item label="问题标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="请简要描述您的问题，例如：宿舍水龙头漏水"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="问题分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择问题分类" style="width: 100%">
            <el-option label="校园报修" value="校园报修" />
            <el-option label="产品售后" value="产品售后" />
            <el-option label="投诉建议" value="投诉建议" />
            <el-option label="业务咨询" value="业务咨询" />
            <el-option label="账号问题" value="账号问题" />
            <el-option label="其他问题" value="其他问题" />
          </el-select>
        </el-form-item>

        <el-form-item label="所属部门">
          <el-select v-model="form.department" placeholder="请选择对应部门（可选）" style="width: 100%">
            <el-option label="客服中心" value="客服中心" />
            <el-option label="后勤保障部" value="后勤保障部" />
            <el-option label="信息技术部" value="信息技术部" />
            <el-option label="教务办公室" value="教务办公室" />
            <el-option label="学生工作处" value="学生工作处" />
          </el-select>
        </el-form-item>

        <el-form-item label="优先级" prop="priority">
          <el-radio-group v-model="form.priority">
            <el-radio value="LOW">
              <el-tag type="info" size="small">低</el-tag>
              <span style="margin-left: 6px; font-size: 13px; color: #64748b">一般咨询</span>
            </el-radio>
            <el-radio value="NORMAL">
              <el-tag size="small">普通</el-tag>
              <span style="margin-left: 6px; font-size: 13px; color: #64748b">24h内处理</span>
            </el-radio>
            <el-radio value="HIGH">
              <el-tag type="warning" size="small">高</el-tag>
              <span style="margin-left: 6px; font-size: 13px; color: #64748b">12h内处理</span>
            </el-radio>
            <el-radio value="URGENT">
              <el-tag type="danger" size="small">紧急</el-tag>
              <span style="margin-left: 6px; font-size: 13px; color: #64748b">4h内处理</span>
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="详细描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="6"
            placeholder="请详细描述您遇到的问题，包括问题发生的时间、地点、具体现象等，以便客服更好地帮助您"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="附件上传">
          <el-upload
            action="#"
            :auto-upload="false"
            multiple
            :limit="5"
            :on-exceed="handleExceed"
            class="upload-area"
          >
            <el-button type="primary" plain>
              <el-icon><Upload /></el-icon>
              选择文件
            </el-button>
            <template #tip>
              <div class="el-upload__tip">支持 jpg/png/pdf 格式，单文件不超过 10MB，最多上传 5 个</div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
            <el-icon><Check /></el-icon>
            提交工单
          </el-button>
          <el-button size="large" @click="handleReset">
            <el-icon><RefreshRight /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 提交成功弹窗 -->
    <el-dialog v-model="successVisible" title="工单提交成功" width="480px">
      <div class="success-content">
        <el-result icon="success" title="提交成功" sub-title="客服人员会尽快处理您的问题">
          <template #extra>
            <div class="ticket-info">
              <p><strong>工单号：</strong>#{{ createdTicket?.id }}</p>
              <p><strong>问题标题：</strong>{{ createdTicket?.title }}</p>
              <p><strong>当前状态：</strong>
                <el-tag type="warning">待接单</el-tag>
              </p>
            </div>
          </template>
        </el-result>
      </div>
      <template #footer>
        <el-button @click="goToList">查看我的工单</el-button>
        <el-button type="primary" @click="createAgain">继续创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Upload, Check, RefreshRight } from '@element-plus/icons-vue'
import { createTicket } from '../api/ticket'
import type { Ticket } from '../api/ticket'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const successVisible = ref(false)
const createdTicket = ref<Ticket | null>(null)

const form = reactive({
  title: '',
  category: '',
  department: '',
  priority: 'NORMAL',
  description: ''
})

const rules: FormRules = {
  title: [
    { required: true, message: '请输入问题标题', trigger: 'blur' },
    { min: 5, max: 50, message: '标题长度在 5-50 字之间', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择问题分类', trigger: 'change' }
  ],
  description: [
    { required: true, message: '请填写详细描述', trigger: 'blur' },
    { min: 10, message: '描述不能少于 10 个字', trigger: 'blur' }
  ]
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const res = await createTicket({
        title: form.title,
        description: form.description,
        category: form.category,
        department: form.department,
        priority: form.priority
      })
      createdTicket.value = res
      successVisible.value = true
    } catch (e: any) {
      ElMessage.error(e.message || '提交失败')
    } finally {
      submitting.value = false
    }
  })
}

function handleReset() {
  formRef.value?.resetFields()
  form.priority = 'NORMAL'
}

function handleExceed() {
  ElMessage.warning('最多只能上传 5 个文件')
}

function goToList() {
  successVisible.value = false
  router.push('/tickets')
}

function createAgain() {
  successVisible.value = false
  handleReset()
}
</script>

<style scoped>
.create-page {
  max-width: 720px;
  margin: 0 auto;
}

.form-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.card-title { font-weight: 600; font-size: 18px; color: #1e293b; }
.card-subtitle { font-size: 13px; color: #64748b; font-weight: normal; }

.upload-area :deep(.el-upload) {
  display: block;
}

.success-content { text-align: center; }
.ticket-info {
  text-align: left;
  background: #f8fafc;
  padding: 16px 20px;
  border-radius: 8px;
  margin-top: 16px;
}
.ticket-info p {
  margin: 8px 0;
  color: #475569;
  font-size: 14px;
}
</style>
