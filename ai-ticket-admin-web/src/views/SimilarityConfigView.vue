<template>
  <div class="similarity-config">
    <el-card class="config-card">
      <template #header>
        <span class="card-title">语义匹配配置</span>
      </template>

      <el-form label-width="160px" style="max-width: 640px">
        <el-form-item label="语义检索开关">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="禁用" />
          <div class="form-tip">开启后将使用大模型 Embedding 进行语义相似度匹配</div>
        </el-form-item>

        <el-form-item label="相似度阈值">
          <el-slider
            v-model="form.threshold"
            :min="0.5" :max="0.95" :step="0.01"
            :marks="{ 0.6: '0.6', 0.7: '0.7', 0.75: '0.75', 0.8: '0.8', 0.9: '0.9' }"
            show-input
          />
          <div class="form-tip">高于此阈值的 FAQ 将直接返回答案，低于阈值则转大模型或人工</div>
        </el-form-item>

        <el-form-item label="召回数量">
          <el-input-number v-model="form.topK" :min="1" :max="10" />
          <div class="form-tip">语义检索返回的最相关 FAQ 数量</div>
        </el-form-item>

        <el-form-item label="兜底策略">
          <el-radio-group v-model="form.fallback">
            <el-radio value="llm">调用大模型回答</el-radio>
            <el-radio value="keyword">关键词模糊匹配</el-radio>
            <el-radio value="transfer">直接转人工</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="Embedding 模型">
          <el-select v-model="form.embedModel" style="width: 300px">
            <el-option label="text-embedding-v2 (DeepSeek)" value="text-embedding-v2" />
            <el-option label="text-embedding-v3 (DeepSeek)" value="text-embedding-v3" />
          </el-select>
        </el-form-item>

        <el-form-item label=" ">
          <el-button type="primary" @click="handleSave">保存配置</el-button>
          <el-button @click="handleRebuild">
            <el-icon><Refresh /></el-icon>
            重建向量索引
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 匹配测试 -->
    <el-card class="test-card" style="margin-top: 16px">
      <template #header>
        <span class="card-title">语义匹配测试</span>
      </template>
      <div class="test-area">
        <el-input
          v-model="testQuestion"
          placeholder="输入一个问题，测试语义匹配效果"
          clearable
          @keyup.enter="handleTest"
        >
          <template #append>
            <el-button :loading="testing" @click="handleTest">测试匹配</el-button>
          </template>
        </el-input>

        <div v-if="testResult" class="test-result">
          <el-result v-if="testResult.matched" icon="success" title="匹配成功">
            <template #sub-title>
              匹配方式：<el-tag size="small" type="primary">{{ matchTypeLabel(testResult.matchType) }}</el-tag>
              &nbsp;相似度：<strong>{{ Math.round(testResult.score * 100) }}%</strong>
            </template>
            <div class="matched-faq">
              <div class="faq-q"><strong>Q：</strong>{{ testResult.question }}</div>
              <div class="faq-a"><strong>A：</strong>{{ testResult.answer }}</div>
            </div>
          </el-result>
          <el-result v-else icon="warning" title="未匹配到 FAQ">
            <template #sub-title>该问题未匹配到知识库中的答案，将走兜底策略</template>
          </el-result>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { rebuildFaqVectors, testFaqMatch, getFaqConfig } from '../api/ticket'

const form = reactive({
  enabled: true,
  threshold: 0.72,
  topK: 3,
  fallback: 'llm',
  embedModel: 'text-embedding-v2'
})

const testQuestion = ref('')
const testing = ref(false)
const testResult = ref<any>(null)

onMounted(async () => {
  try {
    const config = await getFaqConfig()
    form.enabled = config.semanticSearchEnabled ?? true
    form.threshold = config.similarityThreshold ?? 0.72
  } catch (e) { /* use defaults */ }
})

function handleSave() {
  // 当前配置由 application.yml 管理，运行时不可通过 API 修改
  ElMessage.info('当前阈值和语义搜索开关由服务端 application.yml 配置管理，如需修改请联系管理员')
}

async function handleRebuild() {
  try {
    const res = await rebuildFaqVectors() as any
    ElMessage.success(`向量重建完成，更新 ${res.rebuilt || 0} 条`)
  } catch (e: any) {
    ElMessage.error(e.message || '向量重建失败')
  }
}

async function handleTest() {
  if (!testQuestion.value.trim()) {
    ElMessage.warning('请输入测试问题')
    return
  }
  testing.value = true
  try {
    testResult.value = await testFaqMatch(testQuestion.value)
  } catch (e: any) {
    ElMessage.error(e.message || '匹配测试失败，请确认 AI 服务是否可用')
    testResult.value = null
  } finally { testing.value = false }
}

function matchTypeLabel(type: string) {
  const map: Record<string, string> = {
    semantic: '语义匹配', keyword: '关键词匹配', fuzzy: '模糊匹配'
  }
  return map[type] || type
}
</script>

<style scoped>
.config-card, .test-card { border-radius: 12px; }
.card-title { font-weight: 600; color: #1e293b; }
.form-tip { font-size: 12px; color: #94a3b8; margin-top: 4px; }
.test-area { max-width: 640px; }
.test-result { margin-top: 20px; }
.matched-faq {
  text-align: left;
  background: #f8fafc;
  padding: 16px;
  border-radius: 8px;
  margin-top: 12px;
}
.faq-q { margin-bottom: 8px; color: #1e293b; }
.faq-a { color: #475569; line-height: 1.7; }
</style>
