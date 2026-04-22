<template>
  <div class="ai-training-page">
    <div class="page-hero">
      <div>
        <p class="page-hero__eyebrow">AI TRAINING STUDIO</p>
        <h1>AI 训练台</h1>
        <p class="page-hero__desc">
          这里直接管理 AI 的运行配置、基础人设和训练提示词。右侧训练对话只读取这段真实聊天记录，不会把提示词再次蒸馏成假样本。
        </p>
      </div>
      <div class="page-hero__actions">
        <el-button plain @click="router.push('/site/config')">返回站点设置</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存并立即生效</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :xl="14">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-card__header">
              <div>
                <strong>当前基础人设</strong>
                <p>这部分来自服务器文件或 classpath，是 AI 的底稿。</p>
              </div>
              <el-tag type="info">{{ aiConfig.personaPromptPath || '未配置' }}</el-tag>
            </div>
          </template>

          <el-input
            :model-value="aiConfig.personaPrompt"
            type="textarea"
            :rows="16"
            readonly
            resize="none"
            placeholder="当前未读取到基础人设文件"
          />
        </el-card>

        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-card__header">
              <div>
                <strong>训练提示词</strong>
                <p>训练对话会根据真实聊天记录更新这里，保存后下一轮请求立即生效。</p>
              </div>
              <el-switch v-model="trainingForm.trainingEnabled" />
            </div>
          </template>

          <el-form :model="trainingForm" label-width="96px">
            <el-form-item label="训练开关">
              <el-switch v-model="trainingForm.trainingEnabled" />
            </el-form-item>

            <el-form-item label="训练内容">
              <el-input
                v-model="trainingForm.trainingPrompt"
                type="textarea"
                :rows="18"
                resize="vertical"
                placeholder="这里会累积对话训练结果，你也可以手动调整。"
              />
            </el-form-item>
          </el-form>

          <div class="editor-actions">
            <el-button @click="trainingForm.trainingPrompt = ''">清空训练词</el-button>
            <el-button type="primary" plain @click="applyLatestDraft" :disabled="!latestDraftPrompt">
              用最新训练草稿覆盖
            </el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="10">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="panel-card__header">
              <div>
                <strong>运行时接口</strong>
                <p>这里改的是线上实际在用的 AI 配置，不需要再改环境变量。</p>
              </div>
            </div>
          </template>

          <el-form :model="runtimeForm" label-width="92px">
            <el-form-item label="Base URL">
              <el-input v-model="runtimeForm.baseUrl" placeholder="例如：https://www.fluapi.com 或 https://api.openai.com/v1" />
            </el-form-item>

            <el-form-item label="API Key">
              <el-input
                v-model="runtimeForm.apiKey"
                type="password"
                show-password
                :placeholder="runtimeForm.apiKeyMasked ? `当前已保存：${runtimeForm.apiKeyMasked}，留空保持不变` : '输入新的 API Key'"
              />
            </el-form-item>

            <el-form-item label="模型">
              <el-input v-model="runtimeForm.model" placeholder="例如：gpt-5.4" />
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="panel-card panel-card--chat">
          <template #header>
            <div class="panel-card__header">
              <div>
                <strong>对话式训练</strong>
                <p>你正常和它聊，它会根据这段真实聊天记录更新训练草稿。</p>
              </div>
              <el-tag type="success">真实记录</el-tag>
            </div>
          </template>

          <div class="training-chat__summary">
            <div class="training-chat__summary-main">
              {{ trainingSummary || '先聊几轮，我再根据真实聊天记录生成训练草稿。' }}
            </div>
            <div v-if="warningTags.length" class="training-chat__tags">
              <el-tag v-for="item in warningTags" :key="item" type="warning" effect="plain" size="small">{{ item }}</el-tag>
            </div>
            <div v-if="missingTags.length" class="training-chat__tags">
              <el-tag v-for="item in missingTags" :key="item" type="info" effect="plain" size="small">{{ item }}</el-tag>
            </div>
          </div>

          <div ref="chatViewportRef" class="training-chat__messages">
            <div
              v-for="item in trainingMessages"
              :key="item.id"
              class="training-chat__message"
              :class="`is-${item.role}`"
            >
              <div class="training-chat__avatar">{{ item.role === 'assistant' ? 'AI' : '我' }}</div>
              <div class="training-chat__bubble">
                <div class="training-chat__meta">
                  <strong>{{ item.role === 'assistant' ? '训练助手' : '我' }}</strong>
                  <span>{{ item.time }}</span>
                </div>
                <p>{{ item.content }}</p>
              </div>
            </div>

            <div v-if="chatLoading" class="training-chat__message is-assistant">
              <div class="training-chat__avatar">AI</div>
              <div class="training-chat__bubble training-chat__bubble--loading">
                <div class="training-chat__meta">
                  <strong>训练助手</strong>
                  <span>处理中</span>
                </div>
                <p>我在继续读取你的表达习惯，顺手更新训练词。</p>
              </div>
            </div>
          </div>

          <div class="training-chat__composer">
            <el-input
              v-model="chatInput"
              type="textarea"
              resize="none"
              :autosize="{ minRows: 3, maxRows: 5 }"
              placeholder="直接跟它聊。比如：我说话别太客服腔，技术问题先给结论，再说步骤。"
              @keydown.enter.exact.prevent="handleTrainingSend"
            />

            <div class="training-chat__actions">
              <span class="training-chat__hint">Enter 发送，Shift + Enter 换行</span>
              <div class="training-chat__buttons">
                <el-button :disabled="chatLoading" @click="resetTrainingChat">重新开始</el-button>
                <el-button type="primary" :loading="chatLoading" @click="handleTrainingSend">
                  发送并自动吸收
                </el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  chatAiTrainingApi,
  getAiConfigApi,
  updateAiRuntimeConfigApi,
  updateAiTrainingConfigApi
} from '@/api/site/config'

interface TrainingMessageItem {
  id: string
  role: 'user' | 'assistant'
  content: string
  time: string
}

const router = useRouter()
const saving = ref(false)
const chatLoading = ref(false)
const latestDraftPrompt = ref('')
const trainingSummary = ref('')
const warningTags = ref<string[]>([])
const missingTags = ref<string[]>([])
const chatViewportRef = ref<HTMLElement>()
const chatInput = ref('')

const aiConfig = reactive({
  personaPromptPath: '',
  personaPrompt: ''
})

const runtimeForm = reactive({
  baseUrl: '',
  apiKey: '',
  apiKeyMasked: '',
  model: ''
})

const trainingForm = reactive({
  trainingPrompt: '',
  trainingEnabled: true
})

const trainingMessages = ref<TrainingMessageItem[]>([
  createMessage('assistant', '你正常和我聊就行。我只根据这段真实聊天记录摸你的语气、边界和表达习惯。')
])

function createMessage(role: 'user' | 'assistant', content: string): TrainingMessageItem {
  return {
    id: `${role}-${Date.now()}-${Math.random().toString(16).slice(2)}`,
    role,
    content,
    time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
}

async function loadConfig() {
  const { data } = await getAiConfigApi()
  aiConfig.personaPromptPath = data?.personaPromptPath || ''
  aiConfig.personaPrompt = data?.personaPrompt || ''
  runtimeForm.baseUrl = data?.baseUrl || ''
  runtimeForm.apiKey = ''
  runtimeForm.apiKeyMasked = data?.apiKeyMasked || ''
  runtimeForm.model = data?.model || ''
  trainingForm.trainingPrompt = data?.trainingPrompt || ''
  trainingForm.trainingEnabled = data?.trainingEnabled !== false
}

async function handleSave() {
  saving.value = true
  try {
    await updateAiRuntimeConfigApi({
      baseUrl: runtimeForm.baseUrl,
      apiKey: runtimeForm.apiKey,
      model: runtimeForm.model
    })
    await updateAiTrainingConfigApi({
      trainingPrompt: trainingForm.trainingPrompt,
      trainingEnabled: trainingForm.trainingEnabled
    })
    ElMessage.success('AI 配置已保存并立即生效')
    await loadConfig()
  } finally {
    saving.value = false
  }
}

async function handleTrainingSend() {
  const content = chatInput.value.trim()
  if (!content || chatLoading.value) {
    return
  }

  trainingMessages.value.push(createMessage('user', content))
  chatInput.value = ''
  chatLoading.value = true
  scrollChatToBottom()

  try {
    const { data } = await chatAiTrainingApi({
      messages: trainingMessages.value.map((item) => ({
        role: item.role,
        content: item.content
      })),
      currentDraftPrompt: trainingForm.trainingPrompt
    })

    if (data?.assistantReply) {
      trainingMessages.value.push(createMessage('assistant', data.assistantReply))
    }
    if (data?.draftPrompt) {
      latestDraftPrompt.value = data.draftPrompt
      trainingForm.trainingPrompt = data.draftPrompt
    }
    trainingSummary.value = data?.summary || ''
    warningTags.value = Array.isArray(data?.warnings) ? data.warnings : []
    missingTags.value = Array.isArray(data?.missingDimensions) ? data.missingDimensions : []
    scrollChatToBottom()
  } catch (error: any) {
    ElMessage.error(error?.message || '训练对话失败，请稍后再试')
  } finally {
    chatLoading.value = false
  }
}

function applyLatestDraft() {
  if (!latestDraftPrompt.value) {
    return
  }
  trainingForm.trainingPrompt = latestDraftPrompt.value
  ElMessage.success('已应用最新训练草稿')
}

function resetTrainingChat() {
  trainingMessages.value = [
    createMessage('assistant', '重新开始吧。你直接按平时的语气和我聊，我会继续从真实聊天里提炼你的风格。')
  ]
  trainingSummary.value = ''
  warningTags.value = []
  missingTags.value = []
  latestDraftPrompt.value = trainingForm.trainingPrompt
}

function scrollChatToBottom() {
  nextTick(() => {
    const el = chatViewportRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

onMounted(async () => {
  await loadConfig()
  latestDraftPrompt.value = trainingForm.trainingPrompt
})
</script>

<style scoped lang="scss">
.ai-training-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 28px 30px;
  border-radius: 24px;
  background: linear-gradient(120deg, rgba(234, 244, 255, 0.96), rgba(245, 248, 255, 0.96));
  box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.16);
}

.page-hero__eyebrow {
  margin: 0 0 8px;
  color: #5b6ee1;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.22em;
}

.page-hero h1 {
  margin: 0;
  font-size: 34px;
  line-height: 1.1;
}

.page-hero__desc {
  max-width: 760px;
  margin: 12px 0 0;
  color: #64748b;
  line-height: 1.75;
}

.page-hero__actions {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

.panel-card {
  border-radius: 22px;
}

.panel-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.panel-card__header strong {
  display: block;
  font-size: 16px;
}

.panel-card__header p {
  margin: 6px 0 0;
  color: #64748b;
  line-height: 1.6;
}

.panel-card--chat :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.editor-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.training-chat__summary {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 16px;
  background: #f8fbff;
}

.training-chat__summary-main {
  color: #334155;
  line-height: 1.7;
}

.training-chat__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.training-chat__messages {
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-height: 520px;
  min-height: 360px;
  padding: 6px 4px 6px 0;
  overflow-y: auto;
}

.training-chat__message {
  display: flex;
  gap: 12px;
}

.training-chat__message.is-user {
  flex-direction: row-reverse;
}

.training-chat__avatar {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: #eef2ff;
  color: #374151;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.training-chat__bubble {
  max-width: min(100%, 92%);
  padding: 14px 16px;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.18);
}

.training-chat__message.is-user .training-chat__bubble {
  background: linear-gradient(135deg, #1d4ed8, #3b82f6);
  color: #ffffff;
}

.training-chat__bubble--loading {
  background: #f8fafc;
}

.training-chat__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 12px;
  color: #94a3b8;
}

.training-chat__message.is-user .training-chat__meta {
  color: rgba(255, 255, 255, 0.72);
}

.training-chat__bubble p {
  margin: 0;
  line-height: 1.72;
  white-space: pre-wrap;
  word-break: break-word;
}

.training-chat__composer {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.training-chat__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.training-chat__hint {
  color: #94a3b8;
  font-size: 12px;
}

.training-chat__buttons {
  display: flex;
  gap: 12px;
}

@media (max-width: 1200px) {
  .page-hero {
    flex-direction: column;
  }
}

@media (max-width: 768px) {
  .page-hero {
    padding: 22px 18px;
    border-radius: 20px;
  }

  .page-hero h1 {
    font-size: 28px;
  }

  .page-hero__actions,
  .editor-actions,
  .training-chat__buttons {
    width: 100%;
    flex-direction: column;
  }

  .training-chat__messages {
    min-height: 300px;
  }

  .training-chat__actions {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
