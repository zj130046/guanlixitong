<template>
  <div class="system-settings">
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card class="setting-card">
          <template #header>
            <div class="card-title">
              <el-icon><Setting /></el-icon>
              基础配置
            </div>
          </template>
          <el-form label-width="140px">
            <el-form-item label="系统名称">
              <el-input v-model="settings.systemName" style="width: 300px" />
            </el-form-item>
            <el-form-item label="系统Logo">
              <el-upload action="#" :auto-upload="false" :show-file-list="false">
                <el-button>上传Logo</el-button>
              </el-upload>
            </el-form-item>
            <el-form-item label="工单默认分类">
              <el-select v-model="settings.defaultCategory" style="width: 200px">
                <el-option label="校园报修" value="校园报修" />
                <el-option label="产品售后" value="产品售后" />
                <el-option label="投诉建议" value="投诉建议" />
                <el-option label="业务咨询" value="业务咨询" />
              </el-select>
            </el-form-item>
            <el-form-item label="工单默认优先级">
              <el-radio-group v-model="settings.defaultPriority">
                <el-radio value="LOW">低</el-radio>
                <el-radio value="NORMAL">普通</el-radio>
                <el-radio value="HIGH">高</el-radio>
                <el-radio value="URGENT">紧急</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card class="setting-card" style="margin-top: 16px">
          <template #header>
            <div class="card-title">
              <el-icon><Clock /></el-icon>
              超时策略
            </div>
          </template>
          <el-form label-width="140px">
            <el-form-item label="紧急工单超时">
              <el-input-number v-model="settings.urgentTimeout" :min="1" :max="72" />
              <span style="margin-left: 8px">小时</span>
            </el-form-item>
            <el-form-item label="高优先级超时">
              <el-input-number v-model="settings.highTimeout" :min="1" :max="168" />
              <span style="margin-left: 8px">小时</span>
            </el-form-item>
            <el-form-item label="普通工单超时">
              <el-input-number v-model="settings.normalTimeout" :min="1" :max="336" />
              <span style="margin-left: 8px">小时</span>
            </el-form-item>
            <el-form-item label="提前预警时间">
              <el-input-number v-model="settings.warningTime" :min="1" :max="24" />
              <span style="margin-left: 8px">小时前提醒</span>
            </el-form-item>
            <el-form-item label="超时提醒方式">
              <el-checkbox-group v-model="settings.notifyTypes">
                <el-checkbox value="system">系统消息</el-checkbox>
                <el-checkbox value="email">邮件通知</el-checkbox>
                <el-checkbox value="sms">短信通知</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card class="setting-card">
          <template #header>
            <div class="card-title">
              <el-icon><Cpu /></el-icon>
              AI 服务配置
            </div>
          </template>
          <el-form label-width="140px">
            <el-form-item label="AI服务提供商">
              <el-select v-model="settings.aiProvider" style="width: 200px">
                <el-option label="DeepSeek" value="deepseek" />
                <el-option label="通义千问" value="qwen" />
                <el-option label="智谱AI" value="zhipu" />
              </el-select>
            </el-form-item>
            <el-form-item label="API Key">
              <el-input v-model="settings.apiKey" type="password" show-password style="width: 320px" />
            </el-form-item>
            <el-form-item label="对话模型">
              <el-select v-model="settings.chatModel" style="width: 200px">
                <el-option label="deepseek-chat" value="deepseek-chat" />
                <el-option label="deepseek-reasoner" value="deepseek-reasoner" />
              </el-select>
            </el-form-item>
            <el-form-item label="Embedding模型">
              <el-select v-model="settings.embedModel" style="width: 200px">
                <el-option label="text-embedding-v2" value="text-embedding-v2" />
              </el-select>
            </el-form-item>
            <el-form-item label="流式输出">
              <el-switch v-model="settings.streamEnabled" />
            </el-form-item>
            <el-form-item label="最大Token数">
              <el-input-number v-model="settings.maxTokens" :min="100" :max="8000" :step="100" />
            </el-form-item>
            <el-form-item label="温度系数">
              <el-slider v-model="settings.temperature" :min="0" :max="2" :step="0.1" style="width: 240px" />
            </el-form-item>
          </el-form>
        </el-card>

        <el-card class="setting-card" style="margin-top: 16px">
          <template #header>
            <div class="card-title">
              <el-icon><Lock /></el-icon>
              安全设置
            </div>
          </template>
          <el-form label-width="140px">
            <el-form-item label="启用违规检测">
              <el-switch v-model="settings.violationCheck" />
            </el-form-item>
            <el-form-item label="单用户每日上限">
              <el-input-number v-model="settings.dailyLimit" :min="10" :max="500" />
              <span style="margin-left: 8px">次</span>
            </el-form-item>
            <el-form-item label="登录失败锁定">
              <el-input-number v-model="settings.loginFailLimit" :min="3" :max="20" />
              <span style="margin-left: 8px">次后锁定</span>
            </el-form-item>
            <el-form-item label="Token有效期">
              <el-input-number v-model="settings.tokenExpire" :min="1" :max="30" />
              <span style="margin-left: 8px">天</span>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <div class="action-bar">
      <el-button @click="handleReset">重置</el-button>
      <el-button type="primary" @click="handleSave">保存配置</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Setting, Clock, Cpu, Lock } from '@element-plus/icons-vue'

const settings = reactive({
  systemName: 'AI 智能客服工单系统',
  defaultCategory: '业务咨询',
  defaultPriority: 'NORMAL',
  urgentTimeout: 4,
  highTimeout: 12,
  normalTimeout: 24,
  warningTime: 4,
  notifyTypes: ['system', 'email'],
  aiProvider: 'deepseek',
  apiKey: 'sk-xxxxx',
  chatModel: 'deepseek-chat',
  embedModel: 'text-embedding-v2',
  streamEnabled: true,
  maxTokens: 2000,
  temperature: 0.7,
  violationCheck: true,
  dailyLimit: 100,
  loginFailLimit: 5,
  tokenExpire: 7,
})

function handleSave() {
  ElMessage.success('配置已保存')
}
function handleReset() {
  ElMessage.info('已重置为默认配置')
}
</script>

<style scoped>
.setting-card { border-radius: 12px; }
.card-title {
  display: flex; align-items: center; gap: 8px;
  font-weight: 600; color: #1e293b;
}
.action-bar {
  position: sticky; bottom: 0;
  background: #fff; padding: 16px;
  border-radius: 12px; margin-top: 16px;
  display: flex; justify-content: flex-end; gap: 12px;
  box-shadow: 0 -2px 8px rgba(0,0,0,0.06);
}
</style>
