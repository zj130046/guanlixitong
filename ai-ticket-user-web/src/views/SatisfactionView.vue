<template>
  <div class="satisfaction-page">
    <el-card class="rate-card">
      <div v-if="submitted" class="result-view">
        <el-result icon="success" title="感谢您的评价！" sub-title="您的反馈是我们不断进步的动力">
          <template #extra>
            <el-button type="primary" @click="$router.push('/tickets')">
              返回工单列表
            </el-button>
          </template>
        </el-result>
      </div>

      <template v-else>
        <div class="rate-header">
          <el-icon :size="48" color="#f59e0b"><Star /></el-icon>
          <h2>服务满意度评价</h2>
          <p>请对本次工单处理服务进行评价，您的反馈非常重要</p>
        </div>

        <div v-if="ticket" class="ticket-info">
          <div class="info-row">
            <span class="label">工单号</span>
            <span class="value">#{{ ticket.id }}</span>
          </div>
          <div class="info-row">
            <span class="label">问题标题</span>
            <span class="value">{{ ticket.title }}</span>
          </div>
          <div class="info-row">
            <span class="label">处理人</span>
            <span class="value">{{ ticket.assignee || '系统' }}</span>
          </div>
        </div>

        <el-form :model="form" label-position="top">
          <el-form-item label="服务评分">
            <el-rate
              v-model="form.score"
              :max="5"
              size="large"
              show-text
              :texts="rateTexts"
              text-color="#ff6b00"
            />
          </el-form-item>

          <el-form-item label="评价标签">
            <div class="tag-group">
              <el-tag
                v-for="tag in tags"
                :key="tag"
                :type="selectedTags.includes(tag) ? 'primary' : 'info'"
                effect="plain"
                class="rate-tag"
                @click="toggleTag(tag)"
              >
                {{ tag }}
              </el-tag>
            </div>
          </el-form-item>

          <el-form-item label="补充意见">
            <el-input
              v-model="form.comment"
              type="textarea"
              :rows="4"
              placeholder="请描述您的具体感受或建议（选填）"
              maxlength="300"
              show-word-limit
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              style="width: 100%"
              :loading="submitting"
              @click="handleSubmit"
            >
              提交评价
            </el-button>
          </el-form-item>
        </el-form>
      </template>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Star } from '@element-plus/icons-vue'
import { getTicket, submitSatisfaction } from '../api/ticket'
import type { Ticket } from '../api/ticket'

const route = useRoute()
const ticket = ref<Ticket | null>(null)
const submitting = ref(false)
const submitted = ref(false)

const form = reactive({
  score: 5,
  comment: ''
})

const rateTexts = ['很差', '较差', '一般', '满意', '非常满意']

const tags = [
  '响应迅速', '态度友好', '专业到位', '解决彻底',
  '沟通顺畅', '流程便捷', '服务贴心', '有待改进'
]
const selectedTags = ref<string[]>([])

function toggleTag(tag: string) {
  const idx = selectedTags.value.indexOf(tag)
  if (idx > -1) {
    selectedTags.value.splice(idx, 1)
  } else {
    selectedTags.value.push(tag)
  }
}

onMounted(async () => {
  const id = route.params.ticketId as string
  if (id) {
    try {
      ticket.value = await getTicket(id)
    } catch (e) {
      ElMessage.error('工单信息加载失败')
    }
  }
})

async function handleSubmit() {
  if (!form.score) {
    ElMessage.warning('请选择评分')
    return
  }
  const ticketId = ticket?.value?.id || Number(route.params.ticketId)
  if (!ticketId) {
    ElMessage.error('缺少工单信息')
    return
  }

  submitting.value = true
  try {
    const comment = selectedTags.value.length
      ? `【${selectedTags.value.join('、')}】${form.comment}`
      : form.comment

    await submitSatisfaction({
      ticketId,
      score: form.score,
      comment
    })
    submitted.value = true
  } catch (e: any) {
    ElMessage.error(e.message || '提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.satisfaction-page {
  max-width: 560px;
  margin: 40px auto;
}

.rate-card {
  border-radius: 16px;
}

.rate-header {
  text-align: center;
  padding: 20px 0 30px;
}
.rate-header h2 {
  margin: 12px 0 6px;
  font-size: 22px;
  color: #1e293b;
}
.rate-header p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

.ticket-info {
  background: #f8fafc;
  border-radius: 10px;
  padding: 16px 20px;
  margin-bottom: 24px;
}
.info-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 14px;
}
.info-row .label { color: #64748b; }
.info-row .value { color: #1e293b; font-weight: 500; }

.tag-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.rate-tag {
  cursor: pointer;
  padding: 6px 14px;
  font-size: 13px;
  transition: all 0.2s;
}
.rate-tag:hover {
  transform: translateY(-1px);
}

.result-view {
  padding: 20px 0;
}
</style>
