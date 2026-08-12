<template>
  <div class="faq-page">
    <div class="faq-layout">
      <!-- 左侧分类 -->
      <div class="category-sidebar">
        <div class="search-box">
          <el-input
            v-model="keyword"
            placeholder="搜索问题..."
            clearable
            @input="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
        <div class="category-list">
          <div
            :class="['category-item', { active: !activeCategory }]"
            @click="activeCategory = null"
          >
            <el-icon><Collection /></el-icon>
            <span>全部问题</span>
            <el-badge v-if="totalCount" :value="totalCount" class="cat-badge" />
          </div>
          <div
            v-for="cat in categories"
            :key="cat.id"
            :class="['category-item', { active: activeCategory === cat.id }]"
            @click="selectCategory(cat.id)"
          >
            <el-icon><Document /></el-icon>
            <span>{{ cat.name }}</span>
          </div>
        </div>
      </div>

      <!-- 右侧 FAQ 列表 -->
      <div class="faq-content">
        <div class="content-header">
          <h2>{{ currentCategoryName }}</h2>
          <el-button type="primary" plain @click="goToChat">
            <el-icon><ChatDotRound /></el-icon>
            没找到答案？问 AI
          </el-button>
        </div>

        <div v-loading="loading" class="faq-list">
          <el-collapse v-if="faqList.length" accordion>
            <el-collapse-item
              v-for="faq in faqList"
              :key="faq.id"
              :name="String(faq.id)"
            >
              <template #title>
                <div class="faq-title">
                  <el-icon class="q-icon"><HelpFilled /></el-icon>
                  <span>{{ faq.question }}</span>
                </div>
              </template>
              <div class="faq-answer">
                <p>{{ faq.answer }}</p>
                <div class="faq-actions">
                  <div class="helpful">
                    <span>这个回答有帮助吗？</span>
                    <el-button size="small" text @click="handleHelpful(faq.id, true)">
                      <el-icon><Top /></el-icon>
                      有帮助
                    </el-button>
                    <el-button size="small" text @click="handleHelpful(faq.id, false)">
                      <el-icon><Bottom /></el-icon>
                      没帮助
                    </el-button>
                  </div>
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>

          <el-empty v-else description="暂无相关问题" />
        </div>

        <div class="pagination" v-if="total > 0">
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="size"
            :total="total"
            layout="prev, pager, next"
            background
            small
            @current-change="loadFaqs"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Search, Collection, Document, ChatDotRound,
  HelpFilled, Top, Bottom
} from '@element-plus/icons-vue'
import { getCategories, getFaqEntries, submitFaqFeedback } from '../api/faq'
import type { FaqCategory, FaqEntry } from '../api/faq'

const router = useRouter()
const loading = ref(false)
const keyword = ref('')
const activeCategory = ref<number | null>(null)
const categories = ref<FaqCategory[]>([])
const faqList = ref<FaqEntry[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const totalCount = ref(0)

const currentCategoryName = computed(() => {
  if (keyword.value) return `搜索："${keyword.value}"`
  if (!activeCategory.value) return '全部常见问题'
  const cat = categories.value.find(c => c.id === activeCategory.value)
  return cat?.name || '全部问题'
})

onMounted(() => {
  loadCategories()
  loadFaqs()
})

async function loadCategories() {
  try {
    categories.value = await getCategories() as unknown as FaqCategory[]
  } catch (e) {
    // ignore
  }
}

async function loadFaqs() {
  loading.value = true
  try {
    const res = await getFaqEntries({
      page: page.value,
      size: size.value,
      keyword: keyword.value || undefined,
      categoryId: activeCategory.value || undefined
    }) as any
    faqList.value = res.records || res || []
    total.value = res.total || faqList.value.length
    if (!activeCategory.value && !keyword.value) {
      totalCount.value = res.total || faqList.value.length
    }
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}

function selectCategory(id: number) {
  activeCategory.value = activeCategory.value === id ? null : id
  page.value = 1
  loadFaqs()
}

function handleSearch() {
  page.value = 1
  loadFaqs()
}

function goToChat() {
  router.push('/chat')
}

async function handleHelpful(id: number, helpful: boolean) {
  try {
    await submitFaqFeedback(id, helpful)
    ElMessage.success(helpful ? '感谢您的反馈！' : '很抱歉未能帮到您，您可以去 AI 咨询获取更详细的答案。')
  } catch (e) {
    ElMessage.success(helpful ? '感谢您的反馈！' : '很抱歉未能帮到您，您可以去 AI 咨询获取更详细的答案。')
  }
}
</script>

<style scoped>
.faq-page {
  min-height: 600px;
}

.faq-layout {
  display: flex;
  gap: 20px;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}

.category-sidebar {
  width: 240px;
  background: #f8fafc;
  border-right: 1px solid #e2e8f0;
  padding: 20px 16px;
  flex-shrink: 0;
}

.search-box { margin-bottom: 16px; }

.category-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  color: #475569;
  font-size: 14px;
  margin-bottom: 4px;
  transition: all 0.2s;
}
.category-item:hover {
  background: #e2e8f0;
  color: #1e293b;
}
.category-item.active {
  background: #dbeafe;
  color: #1d4ed8;
  font-weight: 500;
}
.cat-badge { margin-left: auto; }

.faq-content {
  flex: 1;
  padding: 24px 28px;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
}
.content-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.faq-list { min-height: 300px; }

.faq-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 500;
  color: #1e293b;
}
.q-icon { color: #3b82f6; }

.faq-answer {
  color: #475569;
  line-height: 1.8;
  padding-left: 26px;
}
.faq-answer p { margin: 0 0 12px 0; }

.faq-actions {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e2e8f0;
}
.helpful {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #64748b;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
