<template>
  <div class="ai-conversation" :class="[{ 'is-embedded': embedded, 'is-mobile-compact': isCompactMobile, 'is-home-mode': isHomeMode, 'without-sidebar': !shouldShowDesktopSidebar }, `theme-${theme}`]">
    <aside v-if="shouldShowDesktopSidebar" class="ai-sidebar" :class="{ 'is-open': mobileSidebarOpen }">
      <div class="ai-sidebar__header">
        <div>
          <p class="ai-sidebar__eyebrow">AI Workspace</p>
          <h2>会话</h2>
        </div>
        <button class="ghost-btn" @click="startNewSession">新建</button>
      </div>

      <div class="ai-sidebar__list">
        <article
          v-for="session in sessionCards"
          :key="session.id"
          class="session-card"
          :class="{ 'is-active': currentSessionId === session.id }"
        >
          <button class="session-card__main" @click="selectSession(session.id)">
            <span class="session-card__title">{{ session.title || '新对话' }}</span>
            <span class="session-card__time">{{ formatSessionTime(session.lastMessageAt || session.createTime) }}</span>
          </button>
          <button
            class="session-card__delete"
            :disabled="deletingSessionId === session.id"
            @click.stop="handleDeleteSession(session)"
          >
            <i class="fas fa-trash-alt"></i>
          </button>
        </article>

        <div v-if="!sessionCards.length" class="session-empty">
          还没有历史会话
        </div>
      </div>
    </aside>

    <div v-if="showSidebar && !isCompactMobile && mobileSidebarOpen" class="mobile-mask" @click="mobileSidebarOpen = false"></div>

    <section class="ai-shell">
      <header v-if="!isCompactMobile" class="ai-shell__header">
        <div class="ai-shell__title">
          <button v-if="!embedded && !isCompactMobile" class="feature-chip feature-chip--ghost ai-shell__home-btn" @click="goHome">
            <i class="fas fa-house"></i>
            <span class="feature-chip__label">返回主页</span>
          </button>
          <button v-if="showSidebar && !isCompactMobile" class="mobile-toggle" @click="mobileSidebarOpen = true">
            <i class="fas fa-bars"></i>
          </button>
          <div class="ai-shell__title-copy">
            <p class="ai-shell__eyebrow">{{ embedded ? '悬浮助手' : 'Boylu AI' }}</p>
            <h1>{{ displayTitle }}</h1>
          </div>
        </div>

        <div v-if="!isCompactMobile" class="ai-shell__actions">
          <button
            v-for="mode in modeOptions"
            :key="mode.key"
            class="feature-chip feature-chip--mode"
            :class="{ 'is-active': activeMode === mode.key }"
            @click="selectMode(mode.key)"
          >
            <i :class="mode.icon"></i>
            <span class="feature-chip__copy">
              <span class="feature-chip__label">{{ mode.label }}</span>
              <span class="feature-chip__hint">{{ mode.short }}</span>
            </span>
          </button>
          <button v-if="embedded" class="ghost-btn" @click="openFullPage">
            全页
          </button>
        </div>
      </header>

      <div v-if="!isLoggedIn" class="auth-state">
        <div class="auth-state__badge">AI</div>
        <h3>登录后再继续提问</h3>
        <p>AI 助手需要登录态，才能读取你的会话、附件和站内内容。</p>
        <button class="primary-btn" @click="goLogin">去登录</button>
      </div>

      <template v-else>
        <section v-if="isCompactMobile && showSidebar" v-show="mobileView === 'sessions'" class="mobile-session-panel">
          <div class="mobile-session-panel__header">
            <strong>最近会话</strong>
            <span>{{ sessionCards.length }} 条</span>
          </div>

          <div class="ai-sidebar__list mobile-session-panel__list">
            <article
              v-for="session in sessionCards"
              :key="session.id"
              class="session-card"
              :class="{ 'is-active': currentSessionId === session.id }"
            >
              <button class="session-card__main" @click="selectSession(session.id)">
                <span class="session-card__title">{{ session.title || '新对话' }}</span>
                <span class="session-card__time">{{ formatSessionTime(session.lastMessageAt || session.createTime) }}</span>
              </button>
              <button
                class="session-card__delete"
                :disabled="deletingSessionId === session.id"
                @click.stop="handleDeleteSession(session)"
              >
                <i class="fas fa-trash-alt"></i>
              </button>
            </article>

            <div v-if="!sessionCards.length" class="session-empty">
              还没有历史会话
            </div>
          </div>
        </section>

        <main v-show="!isCompactMobile || mobileView === 'chat'" ref="messageContainer" class="ai-messages" @scroll="handleMessageScroll">
          <div v-if="!normalizedMessages.length" class="empty-state">
            <div class="empty-state__orb">
              <div class="empty-state__orb-ring"></div>
            </div>
            <div class="empty-state__badge">{{ currentModeOption.badge }}</div>
            <h3>{{ embedded ? `${currentModeOption.label}已就绪` : currentModeOption.emptyTitle }}</h3>
            <p>{{ currentModeOption.emptyDescription }}</p>

            <div class="starter-grid">
              <button
                v-for="starter in currentModeOption.starters"
                :key="starter.title"
                class="starter-card"
                @click="applyStarter(starter.prompt, currentModeOption.key)"
              >
                <i :class="starter.icon"></i>
                <strong>{{ starter.title }}</strong>
                <span>{{ starter.description }}</span>
              </button>
            </div>
          </div>

          <article
            v-for="message in normalizedMessages"
            :key="message.localKey"
            class="chat-message"
            :class="`chat-message--${message.role}`"
          >
            <div class="chat-message__avatar">
              <span v-if="message.role === 'assistant'">AI</span>
              <span v-else>我</span>
            </div>

            <div class="chat-message__body">
              <div class="chat-message__meta">
                <span class="chat-message__name">{{ message.role === 'assistant' ? 'Boylu AI' : '我' }}</span>
                <span class="chat-message__time">{{ formatMessageTime(message.createTime) }}</span>
              </div>

              <div
                v-if="message.role === 'assistant'"
                class="chat-message__bubble chat-message__bubble--rich markdown-body"
                v-html="renderMarkdown(message.content)"
              ></div>
              <div v-else class="chat-message__bubble chat-message__bubble--plain">
                {{ message.content }}
              </div>

              <div v-if="message.meta.attachments.length" class="attachment-list">
                <a
                  v-for="(file, index) in message.meta.attachments"
                  :key="`${message.localKey}-attachment-${index}`"
                  class="attachment-card"
                  :href="file.url || 'javascript:void(0)'"
                  target="_blank"
                  rel="noreferrer"
                >
                  <span class="attachment-card__icon">
                    <i :class="resolveAttachmentIcon(file)"></i>
                  </span>
                  <span class="attachment-card__content">
                    <strong>{{ file.name || '未命名附件' }}</strong>
                    <small>{{ formatAttachmentMeta(file) }}</small>
                  </span>
                </a>
              </div>

              <div v-if="message.meta.articles.length" class="source-panel">
                <div class="source-panel__title">
                  <i class="fas fa-link"></i>
                  <span>本次回答引用的站内文章</span>
                </div>
                <router-link
                  v-for="source in message.meta.articles"
                  :key="`${message.localKey}-source-${source.id}`"
                  class="source-card"
                  :to="`/post/${source.id}`"
                >
                  <span class="source-card__label">文章</span>
                  <span class="source-card__title">{{ source.title }}</span>
                </router-link>
              </div>
            </div>
          </article>
        </main>

        <footer v-show="!isCompactMobile || mobileView === 'chat'" class="composer">
          <div v-if="uploadedFiles.length" class="composer__files">
            <div
              v-for="(file, index) in uploadedFiles"
              :key="`${file.url || file.name}-${index}`"
              class="composer-file"
            >
              <span class="composer-file__icon"><i :class="resolveAttachmentIcon(file)"></i></span>
              <span class="composer-file__name">{{ file.name }}</span>
              <button class="composer-file__remove" @click="removeFile(index)">
                <i class="fas fa-times"></i>
              </button>
            </div>
          </div>

          <template v-if="isCompactMobile">
            <div v-show="mobileToolsOpen" class="mobile-mode-menu">
              <button
                v-for="mode in modeOptions"
                :key="mode.key"
                class="mobile-mode-menu__item"
                :class="{ 'is-active': activeMode === mode.key }"
                @click="selectMode(mode.key)"
              >
                <span class="mobile-mode-menu__icon">
                  <i :class="mode.icon"></i>
                </span>
                <span class="mobile-mode-menu__copy">
                  <strong>{{ mode.label }}</strong>
                  <small>{{ mode.short }}</small>
                </span>
              </button>
            </div>

            <div class="mobile-composer__row">
              <button class="mobile-mode-trigger" :class="{ 'is-open': mobileToolsOpen }" @click="toggleMobileTools">
                <span class="mobile-mode-trigger__orb">
                  <i :class="currentModeOption.icon"></i>
                </span>
                <span class="mobile-mode-trigger__label">{{ currentModeOption.label }}</span>
                <i class="fas fa-chevron-up mobile-mode-trigger__chevron"></i>
              </button>

              <div class="composer__input-wrap composer__input-wrap--mobile">
                <textarea
                  ref="textareaRef"
                  v-model.trim="inputValue"
                  class="composer__input composer__input--mobile"
                  :placeholder="streaming ? 'AI 正在回复...' : inputPlaceholder"
                  :disabled="streaming || uploading"
                  @keydown.enter.exact.prevent="handleSendMessage"
                ></textarea>
              </div>

              <label class="mobile-icon-btn" :class="{ 'is-disabled': streaming || uploading }">
                <input
                  ref="fileInputRef"
                  type="file"
                  multiple
                  class="toolbar-btn__input"
                  accept=".txt,.md,.json,.csv,.pdf,.docx"
                  :disabled="streaming || uploading"
                  @change="handleUploadFile"
                >
                <i class="fas fa-paperclip"></i>
              </label>

              <button class="mobile-send-btn" :disabled="streaming || uploading || !canSend" @click="handleSendButtonClick">
                <span class="send-btn__ripples">
                  <span
                    v-for="ripple in sendRipples"
                    :key="ripple.key"
                    class="send-btn__ripple"
                    :style="{
                      left: `${ripple.x}px`,
                      top: `${ripple.y}px`,
                      width: `${ripple.size}px`,
                      height: `${ripple.size}px`
                    }"
                  ></span>
                </span>
                <i class="fas fa-arrow-up"></i>
              </button>
            </div>
          </template>

          <template v-else>
            <div class="composer__input-wrap">
              <textarea
                ref="textareaRef"
                v-model.trim="inputValue"
                class="composer__input"
                :placeholder="streaming ? 'AI 正在回复...' : inputPlaceholder"
                :disabled="streaming || uploading"
                @keydown.enter.exact.prevent="handleSendMessage"
              ></textarea>
            </div>

            <div class="composer__toolbar">
              <div class="composer__toolbar-left">
                <label class="toolbar-btn" :class="{ 'is-disabled': streaming || uploading }">
                  <input
                    ref="fileInputRef"
                    type="file"
                    multiple
                    class="toolbar-btn__input"
                    accept=".txt,.md,.json,.csv,.pdf,.docx"
                    :disabled="streaming || uploading"
                    @change="handleUploadFile"
                  >
                  <i class="fas fa-paperclip"></i>
                  <span>{{ uploading ? '上传中...' : '上传文本附件' }}</span>
                </label>

                <div class="toolbar-tip">
                  {{ toolbarTipText }}
                </div>
              </div>

              <button class="send-btn" :disabled="streaming || uploading || !canSend" @click="handleSendButtonClick">
                <span class="send-btn__surface">
                  <span class="send-btn__ripples">
                    <span
                      v-for="ripple in sendRipples"
                      :key="ripple.key"
                      class="send-btn__ripple"
                      :style="{
                        left: `${ripple.x}px`,
                        top: `${ripple.y}px`,
                        width: `${ripple.size}px`,
                        height: `${ripple.size}px`
                      }"
                    ></span>
                  </span>
                  <span class="send-btn__label">{{ streaming ? '回复中' : '发送' }}</span>
                  <span class="send-btn__icon-shell">
                    <i class="fas fa-arrow-up"></i>
                  </span>
                </span>
              </button>
            </div>
          </template>
        </footer>
      </template>
    </section>
  </div>
</template>

<script>
import { renderMarkdown } from '@/utils/markdown'
import { uploadFileApi } from '@/api/file'
import {
  deleteAiSessionApi,
  getAiSessionDetailApi,
  getAiSessionListApi,
  sendAiMessageApi,
  streamAiMessageApi
} from '@/api/ai'

const MODE_OPTIONS = [
  {
    key: 'chat',
    badge: 'Chat Mode',
    label: '聊天',
    short: '自然闲聊直答',
    summary: '不走站内检索，也不强行拉成长分析，按正常聊天的方式直接回应你。',
    emptyTitle: '先随便聊一句也行',
    emptyDescription: '适合打招呼、日常闲聊、简单提问，回答会更自然、更像真人对话，不主动翻站内资料。',
    placeholder: '比如：你平时怎么学运维，或者直接跟我闲聊一句',
    toolbarHint: '当前模式：聊天，适合自然闲聊和普通提问',
    icon: 'fas fa-comments',
    payload: {
      searchEnabled: false,
      deepResearchEnabled: false,
      reasonEnabled: false
    },
    starters: [
      {
        title: '轻松聊聊',
        description: '直接进入自然对话，不做额外检索',
        prompt: '你平时除了折腾技术，还喜欢做什么',
        icon: 'fas fa-comment-dots'
      },
      {
        title: '认识一下',
        description: '按你的人设自然介绍，但不过度背人设',
        prompt: '简单介绍一下你自己，别太像客服',
        icon: 'fas fa-user'
      },
      {
        title: '随口提问',
        description: '适合短问题和普通日常交流',
        prompt: '最近在忙什么，学习节奏怎么样',
        icon: 'fas fa-paper-plane'
      }
    ]
  },
  {
    key: 'search',
    badge: 'Search Mode',
    label: '站内搜索',
    short: '优先博客内容',
    summary: '会优先读取博客站内相关文章和页面内容，再结合命中的内容回答你。',
    emptyTitle: '先从博客内容里找答案',
    emptyDescription: '适合问博客写了什么、作者在哪篇文章提过什么、某个主题站内有没有记录。',
    placeholder: '比如：根据站内内容，介绍一下这个博客主要写什么',
    toolbarHint: '当前模式：站内搜索，优先依据博客已有内容回答',
    icon: 'fas fa-search',
    payload: {
      searchEnabled: true,
      deepResearchEnabled: false,
      reasonEnabled: false
    },
    starters: [
      {
        title: '博客导览',
        description: '概括站内主要内容和作者方向',
        prompt: '请根据站内内容介绍一下这个博客主要写什么，作者主要在折腾什么',
        icon: 'fas fa-compass'
      },
      {
        title: '文章定位',
        description: '找出某个主题在站内对应的文章',
        prompt: '站内有没有写过 Nginx、Linux 或运维部署相关内容，帮我按主题归一下',
        icon: 'fas fa-map-signs'
      },
      {
        title: '页面说明',
        description: '按站内已有内容解释当前页面',
        prompt: '结合站内内容，介绍一下当前这个页面的作用和适合看什么',
        icon: 'fas fa-file-alt'
      }
    ]
  },
  {
    key: 'research',
    badge: 'Deep Research',
    label: '深研',
    short: '扩召回并综合证据',
    summary: '会扩大站内召回范围，对高相关片段做综合分析，更适合做主题梳理、方案比较和博客内容深挖。',
    emptyTitle: '把站内内容拉宽再分析',
    emptyDescription: '适合问某个主题在博客里怎么一步步展开、不同文章之间的共性和差异，回答会更像一份站内研究摘要。',
    placeholder: '比如：把站内和 Nginx、Linux、部署相关的内容综合起来，给我一个完整梳理',
    toolbarHint: '当前模式：深研，会扩大召回并综合多条站内证据',
    icon: 'fas fa-microscope',
    payload: {
      searchEnabled: true,
      deepResearchEnabled: true,
      reasonEnabled: true
    },
    starters: [
      {
        title: '主题梳理',
        description: '把站内同一主题内容串起来看',
        prompt: '请把站内跟 Linux、Nginx、部署相关的内容综合起来，帮我梳理出一个清晰脉络',
        icon: 'fas fa-sitemap'
      },
      {
        title: '证据对比',
        description: '综合多篇文章的共同点和差异',
        prompt: '结合站内内容，比较一下你在不同文章里对博客部署和运维思路的变化',
        icon: 'fas fa-balance-scale'
      },
      {
        title: '深度总结',
        description: '输出更完整的站内研究型回答',
        prompt: '围绕站内 AI 和博客改造相关内容，给我一份更完整的总结和下一步建议',
        icon: 'fas fa-book-open'
      }
    ]
  },
  {
    key: 'reason',
    badge: 'Reason Mode',
    label: '推理',
    short: '直接分析判断',
    summary: '更偏向直接判断、拆步骤和给建议，不主打站内检索，适合排错、决策和思路分析。',
    emptyTitle: '更适合让 AI 直接判断',
    emptyDescription: '适合问页面为什么不好用、方案该怎么选、某段代码可能哪里有问题，回答会更偏分析。',
    placeholder: '比如：帮我判断这个页面层级为什么不清晰，并给一个最稳的修改方案',
    toolbarHint: '当前模式：推理，适合排错、判断和步骤分析',
    icon: 'fas fa-brain',
    payload: {
      searchEnabled: false,
      deepResearchEnabled: false,
      reasonEnabled: true
    },
    starters: [
      {
        title: '页面诊断',
        description: '直接分析视觉和布局问题',
        prompt: '帮我分析这个页面为什么层级不清晰，给我一个最稳的改法',
        icon: 'fas fa-object-group'
      },
      {
        title: '代码排错',
        description: '判断问题更可能出在哪一层',
        prompt: '这段前端交互为什么会出现状态错乱，帮我判断更可能是哪一层出了问题',
        icon: 'fas fa-code'
      },
      {
        title: '方案决策',
        description: '把几种做法的利弊说清楚',
        prompt: '如果我要在博客里接 AI，对话页到底该走流式还是普通请求，帮我分析利弊',
        icon: 'fas fa-lightbulb'
      }
    ]
  }
]

const MODE_SESSION_STORAGE_KEY = 'boylu-ai-mode-session-map'

function readModeSessionMap() {
  if (typeof window === 'undefined') {
    return {}
  }

  try {
    const raw = window.localStorage.getItem(MODE_SESSION_STORAGE_KEY)
    if (!raw) {
      return {}
    }

    const parsed = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return {}
    }

    return MODE_OPTIONS.reduce((result, mode) => {
      const sessionId = Number(parsed[mode.key])
      if (Number.isFinite(sessionId) && sessionId > 0) {
        result[mode.key] = sessionId
      }
      return result
    }, {})
  } catch (error) {
    return {}
  }
}

function writeModeSessionMap(modeSessionMap) {
  if (typeof window === 'undefined') {
    return
  }

  try {
    window.localStorage.setItem(MODE_SESSION_STORAGE_KEY, JSON.stringify(modeSessionMap || {}))
  } catch (error) {
    // Ignore storage errors.
  }
}

export default {
  name: 'AiConversation',
  props: {
    embedded: {
      type: Boolean,
      default: false
    },
    showSidebar: {
      type: Boolean,
      default: true
    },
    theme: {
      type: String,
      default: 'minimal-light'
    }
  },
  data() {
    return {
      inputValue: '',
      activeMode: 'chat',
      uploadedFiles: [],
      uploading: false,
      streaming: false,
      mobileSidebarOpen: false,
      isMobileViewport: false,
      mobileView: 'chat',
      mobileToolsOpen: false,
      sendRipples: [],
      currentSessionId: null,
      currentSessionTitle: '',
      modeSessionMap: {},
      sessionCards: [],
      messageList: [],
      tempAssistantKey: null,
      shouldAutoScroll: true,
      deletingSessionId: null
    }
  },
  computed: {
    currentUser() {
      return this.$store.state.userInfo || null
    },
    isLoggedIn() {
      return Boolean(this.currentUser?.id)
    },
    isCompactMobile() {
      return this.isMobileViewport && !this.embedded
    },
    shouldShowDesktopSidebar() {
      return this.showSidebar && !this.isCompactMobile && (!this.isHomeMode || this.sessionCards.length > 0)
    },
    displayTitle() {
      if (this.currentSessionTitle) {
        return this.currentSessionTitle
      }
      return this.isHomeMode ? '准备聊点什么？' : '新对话'
    },
    isHomeMode() {
      return !this.normalizedMessages.length
    },
    modeOptions() {
      return MODE_OPTIONS
    },
    currentModeOption() {
      return this.modeOptions.find((item) => item.key === this.activeMode) || this.modeOptions[0]
    },
    inputPlaceholder() {
      return this.currentModeOption?.placeholder || '输入你的问题'
    },
    toolbarTipText() {
      if (this.uploadedFiles.length) {
        return `已附带 ${this.uploadedFiles.length} 个文件 · 当前${this.currentModeOption.label}`
      }
      return this.currentModeOption?.toolbarHint || '当前支持 TXT、MD、JSON、CSV、PDF、DOCX 文本附件'
    },
    canSend() {
      return Boolean(this.inputValue.trim())
    },
    normalizedMessages() {
      return (this.messageList || []).map((message, index) => {
        const meta = this.parseSourceRef(message.sourceRef)
        return {
          ...message,
          localKey: message.id || message.localKey || `${message.role}-${index}`,
          meta
        }
      })
    }
  },
  watch: {
    isLoggedIn(loggedIn) {
      if (loggedIn) {
        this.loadSessionList(true)
        return
      }

      this.resetConversationState()
    }
  },
  created() {
    this.restoreModeSessionMap()
    if (this.isLoggedIn) {
      this.loadSessionList(true)
    }
  },
  mounted() {
    this.syncViewportState()
    window.addEventListener('resize', this.syncViewportState, { passive: true })
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.syncViewportState)
  },
  methods: {
    syncViewportState() {
      this.isMobileViewport = window.innerWidth <= 768
      if (!this.isMobileViewport) {
        this.mobileView = 'chat'
        this.mobileToolsOpen = false
      }
    },
    openMobileSessionPanel() {
      this.mobileView = 'sessions'
      this.mobileToolsOpen = false
    },
    toggleMobileSessionPanel() {
      if (!this.isCompactMobile) {
        return
      }

      this.mobileToolsOpen = false
      this.mobileView = this.mobileView === 'sessions' ? 'chat' : 'sessions'
      if (this.mobileView === 'chat') {
        this.focusComposer()
      }
    },
    toggleMobileTools() {
      this.mobileView = 'chat'
      this.mobileToolsOpen = !this.mobileToolsOpen
    },
    restoreModeSessionMap() {
      this.modeSessionMap = readModeSessionMap()
      this.syncSessionCardModeMeta()
    },
    persistModeSessionMap() {
      writeModeSessionMap(this.modeSessionMap)
    },
    getModeSessionId(modeKey) {
      const sessionId = this.modeSessionMap?.[modeKey]
      return Number.isFinite(Number(sessionId)) && Number(sessionId) > 0 ? Number(sessionId) : null
    },
    getSessionModeKey(sessionId) {
      const normalizedSessionId = Number(sessionId)
      if (!Number.isFinite(normalizedSessionId) || normalizedSessionId <= 0) {
        return ''
      }

      return Object.keys(this.modeSessionMap || {}).find((modeKey) => {
        return Number(this.modeSessionMap[modeKey]) === normalizedSessionId
      }) || ''
    },
    bindSessionToMode(modeKey, sessionId) {
      const normalizedSessionId = Number(sessionId)
      if (!modeKey || !Number.isFinite(normalizedSessionId) || normalizedSessionId <= 0) {
        return
      }

      const nextMap = { ...(this.modeSessionMap || {}) }
      Object.keys(nextMap).forEach((key) => {
        if (key === modeKey || Number(nextMap[key]) === normalizedSessionId) {
          delete nextMap[key]
        }
      })
      nextMap[modeKey] = normalizedSessionId
      this.modeSessionMap = nextMap
      this.persistModeSessionMap()
      this.syncSessionCardModeMeta()
    },
    clearModeSession(modeKey) {
      if (!modeKey || !this.modeSessionMap?.[modeKey]) {
        return
      }

      const nextMap = { ...(this.modeSessionMap || {}) }
      delete nextMap[modeKey]
      this.modeSessionMap = nextMap
      this.persistModeSessionMap()
      this.syncSessionCardModeMeta()
    },
    removeSessionBinding(sessionId) {
      const normalizedSessionId = Number(sessionId)
      if (!Number.isFinite(normalizedSessionId) || normalizedSessionId <= 0) {
        return
      }

      const nextMap = { ...(this.modeSessionMap || {}) }
      let changed = false

      Object.keys(nextMap).forEach((modeKey) => {
        if (Number(nextMap[modeKey]) === normalizedSessionId) {
          delete nextMap[modeKey]
          changed = true
        }
      })

      if (!changed) {
        return
      }

      this.modeSessionMap = nextMap
      this.persistModeSessionMap()
      this.syncSessionCardModeMeta()
    },
    syncSessionCardModeMeta() {
      this.sessionCards = (this.sessionCards || []).map((session) => ({
        ...session,
        modeKey: this.getSessionModeKey(session.id)
      }))
    },
    focusComposer() {
      this.$nextTick(() => {
        if (this.$refs.textareaRef) {
          this.$refs.textareaRef.focus()
        }
      })
    },
    resetConversationState() {
      this.currentSessionId = null
      this.currentSessionTitle = ''
      this.sessionCards = []
      this.messageList = []
      this.uploadedFiles = []
      this.streaming = false
      this.tempAssistantKey = null
    },
    async restoreModeConversation(modeKey = this.activeMode) {
      const mappedSessionId = this.getModeSessionId(modeKey)
      if (mappedSessionId) {
        const restored = await this.selectSession(mappedSessionId, {
          modeKey,
          silent: true
        })
        if (restored) {
          return
        }
      }

      this.startNewSession({
        modeKey,
        clearModeBinding: false
      })
    },
    async selectMode(modeKey) {
      const targetMode = this.modeOptions.find((item) => item.key === modeKey)
      if (!targetMode || this.streaming) {
        return
      }

      this.mobileToolsOpen = false
      this.mobileView = 'chat'

      if (this.activeMode === targetMode.key) {
        this.focusComposer()
        return
      }

      this.activeMode = targetMode.key
      await this.restoreModeConversation(targetMode.key)
      this.focusComposer()
    },
    handleSendButtonClick(event) {
      if (this.streaming || this.uploading || !this.canSend) {
        return
      }
      this.createSendRipple(event)
      this.handleSendMessage()
    },
    createSendRipple(event) {
      const button = event.currentTarget
      if (!button || !button.getBoundingClientRect) {
        return
      }
      const rect = button.getBoundingClientRect()
      const size = Math.max(rect.width, rect.height) * 1.9
      const ripple = {
        key: Date.now() + Math.random(),
        x: event.clientX - rect.left - size / 2,
        y: event.clientY - rect.top - size / 2,
        size
      }
      this.sendRipples = [...this.sendRipples, ripple]
      window.setTimeout(() => {
        this.sendRipples = this.sendRipples.filter((item) => item.key !== ripple.key)
      }, 650)
    },
    async applyStarter(text, modeKey = this.activeMode) {
      await this.selectMode(modeKey)
      this.inputValue = text
      this.mobileToolsOpen = false
      this.mobileView = 'chat'
      this.focusComposer()
    },
    async loadSessionList(restoreModeSession = false) {
      if (!this.isLoggedIn) {
        this.sessionCards = []
        return
      }

      try {
        const { data } = await getAiSessionListApi()
        const records = Array.isArray(data?.records) ? data.records : (Array.isArray(data) ? data : [])
        this.sessionCards = records
        this.syncSessionCardModeMeta()

        if (this.currentSessionId) {
          const exists = this.sessionCards.some((item) => item.id === this.currentSessionId)
          if (!exists) {
            this.removeSessionBinding(this.currentSessionId)
            this.startNewSession({
              modeKey: this.activeMode,
              clearModeBinding: false
            })
          }
          return
        }

        if (restoreModeSession) {
          await this.restoreModeConversation(this.activeMode)
        }
      } catch (error) {
        this.sessionCards = []
      }
    },
    async selectSession(sessionId, options = {}) {
      const targetModeKey = options.modeKey || this.getSessionModeKey(sessionId) || this.activeMode

      try {
        const { data } = await getAiSessionDetailApi(sessionId)
        this.activeMode = targetModeKey
        this.currentSessionId = data?.id || null
        this.currentSessionTitle = data?.title || '新对话'
        this.messageList = Array.isArray(data?.messages) ? data.messages : []
        this.mobileSidebarOpen = false
        this.mobileView = 'chat'
        this.mobileToolsOpen = false
        this.tempAssistantKey = null
        this.shouldAutoScroll = true
        if (this.currentSessionId) {
          this.bindSessionToMode(targetModeKey, this.currentSessionId)
        }
        this.$nextTick(this.scrollToBottom)
        return true
      } catch (error) {
        if (this.getModeSessionId(targetModeKey) === Number(sessionId)) {
          this.clearModeSession(targetModeKey)
        }
        if (!options.silent) {
          this.$message.error(error.message || '加载会话失败')
        }
        return false
      }
    },
    startNewSession(options = {}) {
      const {
        modeKey = this.activeMode,
        clearModeBinding = true
      } = options

      this.activeMode = modeKey
      if (clearModeBinding) {
        this.clearModeSession(modeKey)
      }
      this.currentSessionId = null
      this.currentSessionTitle = ''
      this.messageList = []
      this.tempAssistantKey = null
      this.mobileSidebarOpen = false
      this.mobileView = 'chat'
      this.mobileToolsOpen = false
      this.inputValue = ''
      this.uploadedFiles = []
      this.shouldAutoScroll = true
      this.focusComposer()
    },
    async handleDeleteSession(session) {
      const sessionId = session?.id
      if (!sessionId || this.deletingSessionId === sessionId) {
        return
      }

      try {
        await this.$confirm(`确认删除“${session.title || '新对话'}”吗？`, '删除会话', {
          type: 'warning',
          confirmButtonText: '删除',
          cancelButtonText: '取消'
        })
      } catch (error) {
        return
      }

      this.deletingSessionId = sessionId
      try {
        await deleteAiSessionApi(sessionId)
        this.sessionCards = this.sessionCards.filter((item) => item.id !== sessionId)
        this.removeSessionBinding(sessionId)
        if (this.currentSessionId === sessionId) {
          this.startNewSession({
            modeKey: this.activeMode,
            clearModeBinding: false
          })
        }
        this.$message.success('会话已删除')
      } catch (error) {
        this.$message.error(error.message || '删除会话失败')
      } finally {
        this.deletingSessionId = null
      }
    },
    async handleUploadFile(event) {
      const files = Array.from(event.target.files || [])
      if (!files.length) {
        return
      }

      const supportedFiles = files.filter((file) => this.isSupportedAiAttachment(file))
      if (!supportedFiles.length) {
        this.$message.warning('当前 AI 只支持 TXT、MD、JSON、CSV、PDF、DOCX 文本附件')
        if (this.$refs.fileInputRef) {
          this.$refs.fileInputRef.value = ''
        }
        return
      }
      if (supportedFiles.length !== files.length) {
        this.$message.warning('已自动忽略不支持的附件，当前仅支持 TXT、MD、JSON、CSV、PDF、DOCX')
      }

      this.uploading = true
      try {
        const uploaded = []
        for (const file of supportedFiles) {
          const formData = new FormData()
          formData.append('file', file)
          const excerpt = await this.readAttachmentExcerpt(file)
          const { data } = await uploadFileApi(formData, 'chat')
          uploaded.push({
            name: file.name,
            url: data,
            contentType: file.type,
            size: file.size,
            excerpt
          })
        }
        this.uploadedFiles = [...this.uploadedFiles, ...uploaded]
        this.$message.success(`已上传 ${uploaded.length} 个文件`)
      } catch (error) {
        this.$message.error(error.message || '文件上传失败')
      } finally {
        this.uploading = false
        if (this.$refs.fileInputRef) {
          this.$refs.fileInputRef.value = ''
        }
      }
    },
    removeFile(index) {
      this.uploadedFiles = this.uploadedFiles.filter((_, currentIndex) => currentIndex !== index)
    },
    isSupportedAiAttachment(file) {
      const name = String(file?.name || '').toLowerCase()
      return name.endsWith('.txt')
        || name.endsWith('.md')
        || name.endsWith('.json')
        || name.endsWith('.csv')
        || name.endsWith('.pdf')
        || name.endsWith('.docx')
    },
    async readAttachmentExcerpt(file) {
      const name = (file?.name || '').toLowerCase()
      const type = String(file?.type || '').toLowerCase()
      const canReadText = type.startsWith('text/')
        || type.includes('json')
        || name.endsWith('.txt')
        || name.endsWith('.md')
        || name.endsWith('.json')
        || name.endsWith('.csv')
      if (!canReadText) {
        return ''
      }
      try {
        const text = await file.text()
        return text.slice(0, 1200)
      } catch (error) {
        return ''
      }
    },
    buildSendPayload() {
      const payload = this.currentModeOption?.payload || {}
      return {
        sessionId: this.currentSessionId,
        mode: this.currentModeOption?.key,
        content: this.inputValue,
        searchEnabled: payload.searchEnabled,
        deepResearchEnabled: payload.deepResearchEnabled,
        reasonEnabled: payload.reasonEnabled,
        attachments: this.uploadedFiles
      }
    },
    async handleSendMessage() {
      if (!this.isLoggedIn) {
        this.$message.warning('请先登录后再使用 AI 助手')
        this.goLogin()
        return
      }
      if (!this.canSend || this.streaming || this.uploading) {
        return
      }

      const payload = this.buildSendPayload()
      const draftContent = this.inputValue
      const draftFiles = [...this.uploadedFiles]
      const initialMessageCount = this.messageList.length

      this.streaming = true
      this.tempAssistantKey = `assistant-temp-${Date.now()}`

      let streamDeliveredUser = false

      try {
        await streamAiMessageApi(payload, {
          onSession: (sessionPayload) => {
            this.currentSessionId = sessionPayload?.sessionId || this.currentSessionId
            this.currentSessionTitle = sessionPayload?.title || this.currentSessionTitle || '新对话'
            if (this.currentSessionId) {
              this.bindSessionToMode(this.activeMode, this.currentSessionId)
            }
            this.upsertSessionCard()
          },
          onUser: (userPayload) => {
            streamDeliveredUser = true
            this.messageList.push(userPayload)
            this.messageList.push({
              localKey: this.tempAssistantKey,
              role: 'assistant',
              content: '',
              sourceType: '',
              sourceRef: '',
              createTime: new Date().toISOString()
            })
            this.inputValue = ''
            this.uploadedFiles = []
            this.$nextTick(this.scrollToBottom)
          },
          onDelta: (deltaPayload) => {
            const index = this.messageList.findIndex((item) => item.localKey === this.tempAssistantKey)
            if (index === -1) {
              return
            }
            this.$set(this.messageList, index, {
              ...this.messageList[index],
              content: `${this.messageList[index].content || ''}${deltaPayload?.content || ''}`
            })
            if (this.shouldAutoScroll) {
              this.$nextTick(this.scrollToBottom)
            }
          },
          onDone: async (assistantPayload) => {
            const index = this.messageList.findIndex((item) => item.localKey === this.tempAssistantKey)
            if (index !== -1) {
              this.$set(this.messageList, index, assistantPayload)
            } else {
              this.messageList.push(assistantPayload)
            }
            this.tempAssistantKey = null
            this.streaming = false
            await this.loadSessionList()
            this.$nextTick(this.scrollToBottom)
          },
          onError: (errorPayload) => {
            throw new Error(errorPayload?.message || 'AI 回复失败')
          }
        })
      } catch (error) {
        this.messageList = this.messageList.filter((item) => item.localKey !== this.tempAssistantKey)
        this.tempAssistantKey = null

        if (!streamDeliveredUser && this.shouldFallbackToStandardReply(error)) {
          try {
            this.inputValue = draftContent
            this.uploadedFiles = draftFiles
            await this.sendViaStandardRequest(payload)
            this.$message.warning('流式通道异常，已自动切换为普通回复')
            return
          } catch (fallbackError) {
            this.$message.error(fallbackError.message || '发送失败，请稍后重试')
          }
        } else {
          const recovered = streamDeliveredUser
            ? await this.recoverSessionAfterStreamFailure(initialMessageCount)
            : false
          if (recovered) {
            this.$message.warning('流式连接中断，已自动恢复最近回复')
            return
          }
          this.$message.error(error.message || '发送失败，请稍后重试')
        }

        this.inputValue = draftContent
        this.uploadedFiles = draftFiles
        this.streaming = false
      } finally {
        if (this.streaming) {
          this.streaming = false
        }
      }
    },
    shouldFallbackToStandardReply(error) {
      const message = (error?.message || '').toLowerCase()
      if (!message) {
        return true
      }
      if (message.includes('请先登录')) {
        return false
      }
      return true
    },
    async sendViaStandardRequest(payload) {
      const { data } = await sendAiMessageApi(payload)
      this.currentSessionId = data?.sessionId || this.currentSessionId
      this.currentSessionTitle = data?.title || this.currentSessionTitle || '新对话'
      if (this.currentSessionId) {
        this.bindSessionToMode(this.activeMode, this.currentSessionId)
      }

      if (data?.userMessage) {
        this.messageList.push(data.userMessage)
      }
      if (data?.assistantMessage) {
        this.messageList.push(data.assistantMessage)
      }

      this.inputValue = ''
      this.uploadedFiles = []
      this.tempAssistantKey = null
      this.streaming = false
      await this.loadSessionList()
      this.$nextTick(this.scrollToBottom)
    },
    async recoverSessionAfterStreamFailure(initialMessageCount = 0) {
      if (!this.currentSessionId) {
        return false
      }

      const targetModeKey = this.activeMode
      for (let attempt = 0; attempt < 3; attempt += 1) {
        try {
          const { data } = await getAiSessionDetailApi(this.currentSessionId)
          const messages = Array.isArray(data?.messages) ? data.messages : []
          const lastMessage = messages[messages.length - 1]
          const hasRecoveredAssistantReply = (
            messages.length > initialMessageCount &&
            lastMessage?.role === 'assistant' &&
            String(lastMessage?.content || '').trim()
          )

          if (hasRecoveredAssistantReply) {
            this.currentSessionTitle = data?.title || this.currentSessionTitle || '新对话'
            this.messageList = messages
            this.tempAssistantKey = null
            this.streaming = false
            if (this.currentSessionId) {
              this.bindSessionToMode(targetModeKey, this.currentSessionId)
            }
            await this.loadSessionList()
            this.$nextTick(this.scrollToBottom)
            return true
          }
        } catch (recoverError) {
          // Ignore and continue retrying.
        }

        await new Promise((resolve) => {
          window.setTimeout(resolve, 1200 * (attempt + 1))
        })
      }

      return false
    },
    upsertSessionCard() {
      if (!this.currentSessionId) {
        return
      }
      const payload = {
        id: this.currentSessionId,
        title: this.currentSessionTitle || '新对话',
        lastMessageAt: new Date().toISOString(),
        modeKey: this.activeMode
      }
      const index = this.sessionCards.findIndex((item) => item.id === this.currentSessionId)
      if (index === -1) {
        this.sessionCards.unshift(payload)
      } else {
        this.$set(this.sessionCards, index, { ...this.sessionCards[index], ...payload })
      }
    },
    parseSourceRef(sourceRef) {
      if (!sourceRef) {
        return { articles: [], attachments: [] }
      }
      try {
        const parsed = JSON.parse(sourceRef)
        if (Array.isArray(parsed)) {
          return { articles: parsed, attachments: [] }
        }
        return {
          articles: Array.isArray(parsed.articles) ? parsed.articles : [],
          attachments: Array.isArray(parsed.attachments) ? parsed.attachments : []
        }
      } catch (error) {
        return { articles: [], attachments: [] }
      }
    },
    renderMarkdown(content) {
      return renderMarkdown(this.formatAssistantContent(content || ''))
    },
    formatAssistantContent(content) {
      if (!content) {
        return ''
      }

      const parts = content.replace(/\r\n/g, '\n').split(/(```[\s\S]*?```)/g)
      return parts
        .map((part) => (part.startsWith('```') ? part : this.formatDocumentTextBlock(part)))
        .join('')
        .replace(/\n{3,}/g, '\n\n')
        .trim()
    },
    formatDocumentTextBlock(text) {
      const lines = String(text || '')
        .replace(/\r\n/g, '\n')
        .split('\n')

      const formattedLines = []
      let section1 = 0
      let section2 = 0
      let section3 = 0
      let paragraphBuffer = []

      const flushParagraph = () => {
        if (!paragraphBuffer.length) {
          return
        }
        formattedLines.push(paragraphBuffer.join(' '))
        formattedLines.push('')
        paragraphBuffer = []
      }

      lines.forEach((rawLine) => {
        let line = this.cleanArticleLine(rawLine)

        if (!line) {
          flushParagraph()
          return
        }

        line = line
          .replace(/^(#{1,6})(?!\s)/, '$1 ')
          .replace(/^([•·●▪◦])\s*/, '- ')
          .replace(/^(\d+)[、.)]\s*/, '$1. ')

        const level = this.detectArticleHeadingLevel(line)

        if (level === 1) {
          flushParagraph()
          section1 += 1
          section2 = 0
          section3 = 0
          const title = this.normalizeArticleHeadingText(line, 1)
          formattedLines.push(`# ${section1}. ${title}`)
          formattedLines.push('')
          return
        }

        if (level === 2) {
          flushParagraph()
          if (section1 === 0) {
            section1 = 1
          }
          section2 += 1
          section3 = 0
          const title = this.normalizeArticleHeadingText(line, 2)
          formattedLines.push(`## ${section1}.${section2} ${title}`)
          formattedLines.push('')
          return
        }

        if (level === 3) {
          flushParagraph()
          if (section1 === 0) {
            section1 = 1
          }
          if (section2 === 0) {
            section2 = 1
          }
          section3 += 1
          const title = this.normalizeArticleHeadingText(line, 3)
          formattedLines.push(`### ${section1}.${section2}.${section3} ${title}`)
          formattedLines.push('')
          return
        }

        if (this.isStandaloneBullet(line)) {
          return
        }

        if (this.isListLine(line)) {
          flushParagraph()
          formattedLines.push(this.normalizeListLine(line))
          formattedLines.push('')
          return
        }

        paragraphBuffer.push(line)
      })

      flushParagraph()

      while (formattedLines.length && formattedLines[formattedLines.length - 1] === '') {
        formattedLines.pop()
      }

      return formattedLines.join('\n')
    },
    cleanArticleLine(line) {
      return String(line || '')
        .trim()
        .replace(/[ \t]+/g, ' ')
    },
    detectArticleHeadingLevel(line) {
      if (!line) {
        return 0
      }

      if (/^###\s+.+/.test(line)) {
        return 3
      }
      if (/^##\s+.+/.test(line)) {
        return 2
      }
      if (/^#\s+.+/.test(line)) {
        return 1
      }
      if (/^第[一二三四五六七八九十百千0-9]+[章节部分篇]\s*.*/.test(line)) {
        return 1
      }
      if (/^[一二三四五六七八九十]+[、.]\s*.+/.test(line)) {
        return 2
      }
      if (/^\(?[0-9]+\)?[、.]\s*.+/.test(line)) {
        return 3
      }
      if (this.isShortSectionTitle(line)) {
        return 2
      }
      return 0
    },
    normalizeArticleHeadingText(line, level) {
      let value = String(line || '').trim()

      if (level === 1) {
        value = value.replace(/^#\s*/, '')
        value = value.replace(/^第[一二三四五六七八九十百千0-9]+[章节部分篇]\s*/, '')
      } else if (level === 2) {
        value = value.replace(/^##\s*/, '')
        value = value.replace(/^[一二三四五六七八九十]+[、.]\s*/, '')
      } else if (level === 3) {
        value = value.replace(/^###\s*/, '')
        value = value.replace(/^\(?[0-9]+\)?[、.]\s*/, '')
      }

      return value.trim()
    },
    isShortSectionTitle(line) {
      const value = String(line || '').trim()
      if (!value) {
        return false
      }
      if (/^[#\-*•]/.test(value)) {
        return false
      }
      if (/[，。！？；：:]/.test(value)) {
        return false
      }
      return value.length >= 2 && value.length <= 10
    },
    isStandaloneBullet(line) {
      return /^[-*•·●▪◦]$/.test(String(line || '').trim())
    },
    isListLine(line) {
      return /^[-*•]\s+/.test(line) || /^\d+\.\s+/.test(line)
    },
    normalizeListLine(line) {
      return String(line || '')
        .replace(/^[•·●▪◦]\s*/, '- ')
        .replace(/^\(?(\d+)\)?[、.)]\s*/, '$1. ')
        .trim()
    },
    resolveAttachmentIcon(file) {
      const name = (file?.name || '').toLowerCase()
      const type = (file?.contentType || '').toLowerCase()
      if (type.includes('pdf') || name.endsWith('.pdf')) {
        return 'fas fa-file-pdf'
      }
      if (type.includes('word') || name.endsWith('.doc') || name.endsWith('.docx')) {
        return 'fas fa-file-word'
      }
      if (type.includes('json') || name.endsWith('.json')) {
        return 'fas fa-file-code'
      }
      if (type.includes('csv') || name.endsWith('.csv')) {
        return 'fas fa-file-csv'
      }
      if (name.endsWith('.md')) {
        return 'fas fa-file-alt'
      }
      if (type.startsWith('image/')) {
        return 'fas fa-file-image'
      }
      if (type.startsWith('text/') || name.endsWith('.txt')) {
        return 'fas fa-file-alt'
      }
      return 'fas fa-file'
    },
    formatAttachmentMeta(file) {
      const parts = []
      if (file?.contentType) {
        parts.push(file.contentType)
      }
      if (file?.size) {
        parts.push(`${Math.max(1, Math.round(file.size / 1024))} KB`)
      }
      return parts.join(' · ') || '附件'
    },
    formatSessionTime(value) {
      return this.formatDateTime(value, {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      })
    },
    formatMessageTime(value) {
      return this.formatDateTime(value, {
        hour: '2-digit',
        minute: '2-digit'
      })
    },
    formatDateTime(value, options) {
      if (!value) {
        return ''
      }
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) {
        return ''
      }
      return date.toLocaleString('zh-CN', options)
    },
    handleMessageScroll(event) {
      const target = event.target
      const distanceToBottom = target.scrollHeight - target.scrollTop - target.clientHeight
      this.shouldAutoScroll = distanceToBottom < 80
    },
    scrollToBottom() {
      if (!this.$refs.messageContainer) {
        return
      }
      this.$refs.messageContainer.scrollTop = this.$refs.messageContainer.scrollHeight
    },
    goLogin() {
      this.$router.push({
        path: '/login',
        query: {
          redirect: this.$route.fullPath || '/ai'
        }
      })
    },
    goHome() {
      this.$router.push('/')
    },
    openFullPage() {
      this.$router.push('/ai')
    }
  }
}
</script>

<style lang="scss" scoped>
.ai-conversation {
  display: grid;
  grid-template-columns: 286px minmax(0, 1fr);
  height: 100%;
  min-height: 0;
  border-radius: 32px;
  overflow: hidden;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background:
    radial-gradient(circle at top left, rgba(124, 189, 255, 0.14), transparent 28%),
    radial-gradient(circle at top right, rgba(255, 209, 227, 0.16), transparent 24%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.98));
  box-shadow: 0 30px 80px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(22px);
  font-family:
    "SF Pro Display",
    "PingFang SC",
    "Hiragino Sans GB",
    "Microsoft YaHei",
    "Segoe UI",
    sans-serif;

  &.is-embedded {
    grid-template-columns: minmax(0, 1fr);
    min-height: 0;
    height: 100%;
    border-radius: 24px;
    box-shadow: none;
    background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.98));
  }

  &.without-sidebar {
    grid-template-columns: minmax(0, 1fr);
  }
}

.ai-sidebar {
  padding: 20px 14px;
  background:
    linear-gradient(180deg, rgba(241, 245, 249, 0.88), rgba(248, 250, 252, 0.96));
  color: #0f172a;
  display: flex;
  flex-direction: column;
  gap: 18px;
  border-right: 1px solid rgba(226, 232, 240, 0.92);
}

.ai-sidebar__header,
.ai-shell__header,
.chat-message__meta,
.composer__toolbar,
.composer-file,
.source-panel__title,
.attachment-card,
.ai-shell__title,
.ai-shell__actions {
  display: flex;
  align-items: center;
}

.ai-sidebar__header,
.ai-shell__header,
.composer__toolbar {
  justify-content: space-between;
}

.ai-sidebar__eyebrow,
.ai-shell__eyebrow {
  margin: 0 0 6px;
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #5b86ff;
}

.ai-sidebar__header h2,
.ai-shell__title h1,
.auth-state h3,
.empty-state h3 {
  margin: 0;
  font-family:
    "SF Pro Display",
    "PingFang SC",
    "Hiragino Sans GB",
    "Microsoft YaHei",
    "Segoe UI",
    sans-serif;
}

.ai-sidebar__list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow: auto;
  padding-right: 4px;
}

.session-card {
  display: flex;
  align-items: stretch;
  gap: 8px;
  padding: 0;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.8);
  box-shadow:
    inset 0 0 0 1px rgba(226, 232, 240, 0.96),
    0 10px 24px rgba(15, 23, 42, 0.04);
}

.session-card.is-active {
  background: linear-gradient(135deg, rgba(226, 238, 255, 0.94), rgba(240, 244, 255, 0.9));
  box-shadow:
    inset 0 0 0 1px rgba(137, 180, 255, 0.36),
    0 14px 30px rgba(79, 121, 255, 0.12);
}

.session-card__main {
  flex: 1;
  min-width: 0;
  padding: 14px 12px 14px 16px;
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.session-card__delete {
  width: 42px;
  border: 0;
  border-left: 1px solid rgba(226, 232, 240, 0.9);
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
}

.session-card__delete:hover {
  color: #fca5a5;
}

.session-card__delete[disabled] {
  opacity: 0.5;
  cursor: not-allowed;
}

.session-card__title {
  display: block;
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-card__time,
.toolbar-tip,
.session-empty,
.chat-message__time,
.attachment-card__content small {
  font-size: 12px;
  color: #8b9ab1;
}

.ai-shell {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  min-width: 0;
  min-height: 0;
}

.ai-conversation.is-home-mode .ai-shell__header {
  border-bottom-color: transparent;
  background: transparent;
  backdrop-filter: none;
}

.ai-conversation.is-home-mode .ai-messages {
  justify-content: center;
}

.ai-conversation.is-home-mode .composer {
  width: min(980px, calc(100% - 36px));
  margin: 0 auto 18px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  border-top: 1px solid rgba(226, 232, 240, 0.92);
  border-radius: 28px;
  box-shadow: 0 24px 56px rgba(15, 23, 42, 0.06);
}

.ai-shell__header {
  gap: 16px;
  padding: 20px 24px 14px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.86);
  background: rgba(255, 255, 255, 0.58);
  backdrop-filter: blur(16px);
}

.ai-shell__actions {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.ai-shell__title-copy {
  min-width: 0;
}

.ai-shell__home-btn {
  flex-shrink: 0;
}

.feature-chip,
.ghost-btn,
.primary-btn,
.send-btn,
.toolbar-btn,
.mobile-toggle {
  border: 0;
  cursor: pointer;
}

.feature-chip,
.ghost-btn,
.toolbar-btn,
.mobile-toggle {
  border-radius: 999px;
}

.feature-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.78);
  color: #475569;
  box-shadow: inset 0 0 0 1px rgba(226, 232, 240, 0.9);
}

.feature-chip.is-active {
  background: rgba(232, 240, 255, 0.96);
  color: #245bdf;
  box-shadow: inset 0 0 0 1px rgba(164, 193, 255, 0.8);
}

.feature-chip--mode {
  align-items: flex-start;
  min-width: 134px;
  padding: 10px 14px 10px 12px;
  text-align: left;
}

.feature-chip--mode i {
  margin-top: 2px;
  font-size: 13px;
}

.feature-chip__copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.feature-chip__label {
  font-size: 13px;
  font-weight: 700;
  line-height: 1.18;
}

.feature-chip__hint {
  font-size: 11px;
  line-height: 1.24;
  color: #8b9ab1;
}

.feature-chip--mode.is-active .feature-chip__hint {
  color: #5b78c9;
}

.feature-chip--ghost {
  gap: 6px;
  padding: 10px 14px;
  color: #1f2937;
  background: rgba(255, 255, 255, 0.9);
  box-shadow:
    inset 0 0 0 1px rgba(226, 232, 240, 0.92),
    0 12px 24px rgba(15, 23, 42, 0.08);
}

.mobile-mode-menu,
.mobile-mode-menu__item,
.mobile-mode-menu__copy,
.mobile-composer__row,
.mobile-mode-trigger,
.mobile-mode-trigger__orb,
.mobile-icon-btn,
.mobile-send-btn,
.mobile-session-panel__header {
  display: none;
}

.ghost-btn,
.primary-btn,
.send-btn {
  padding: 10px 16px;
  font-weight: 600;
}

.ghost-btn {
  background: rgba(255, 255, 255, 0.78);
  color: #334155;
  box-shadow: inset 0 0 0 1px rgba(226, 232, 240, 0.9);
}

.primary-btn,
.send-btn {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
}

.mobile-toggle {
  display: none;
  width: 42px;
  height: 42px;
  background: rgba(148, 163, 184, 0.12);
  color: #0f172a;
}

.auth-state,
.empty-state {
  padding: 40px 28px;
  text-align: center;
}

.auth-state p,
.empty-state p {
  margin: 14px auto 0;
  max-width: 460px;
  color: #64748b;
  line-height: 1.8;
}

.auth-state__badge,
.empty-state__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 34px;
  margin: 0 auto 14px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(231, 239, 255, 0.92);
  color: #3b6ff0;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.ai-shell__mode-banner {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  margin: 0 24px;
  padding: 14px 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.68);
  box-shadow:
    inset 0 0 0 1px rgba(226, 232, 240, 0.94),
    0 14px 34px rgba(15, 23, 42, 0.05);
}

.ai-shell__mode-icon {
  width: 44px;
  height: 44px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.14), rgba(95, 208, 255, 0.2));
  color: #2563eb;
  flex-shrink: 0;
}

.ai-shell__mode-copy {
  min-width: 0;
}

.ai-shell__mode-topline {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
}

.ai-shell__mode-badge,
.ai-shell__mode-lock {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
}

.ai-shell__mode-badge {
  background: rgba(231, 239, 255, 0.96);
  color: #3b6ff0;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.ai-shell__mode-lock {
  background: rgba(241, 245, 249, 0.92);
  color: #64748b;
}

.ai-shell__mode-copy strong {
  display: block;
  font-size: 18px;
  color: #0f172a;
}

.ai-shell__mode-copy p {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: #64748b;
}

.empty-state {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.empty-state__orb {
  position: relative;
  width: 92px;
  height: 92px;
  margin-bottom: 22px;
}

.empty-state__orb::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 999px;
  background: linear-gradient(135deg, #2f6fff, #5fd0ff);
  box-shadow: 0 20px 46px rgba(72, 126, 255, 0.24);
}

.empty-state__orb::after {
  content: '';
  position: absolute;
  inset: 23px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
}

.empty-state__orb-ring {
  position: absolute;
  inset: -12px;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(114, 183, 255, 0.18), rgba(114, 183, 255, 0));
  filter: blur(20px);
}

.starter-grid {
  width: 100%;
  max-width: 980px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 26px;
}

.starter-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 18px 16px;
  border: 0;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow:
    inset 0 0 0 1px rgba(226, 232, 240, 0.92),
    0 18px 36px rgba(15, 23, 42, 0.06);
  color: #0f172a;
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.starter-card i {
  font-size: 18px;
  color: #4f7fff;
}

.starter-card strong {
  font-size: 18px;
  font-weight: 700;
}

.starter-card span {
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
}

.starter-card:hover {
  transform: translateY(-2px);
  box-shadow:
    inset 0 0 0 1px rgba(164, 193, 255, 0.7),
    0 22px 42px rgba(79, 127, 255, 0.12);
}

.ai-messages {
  overflow: auto;
  padding: 24px 26px;
  display: flex;
  flex-direction: column;
  gap: 22px;
  min-height: 0;
  background:
    radial-gradient(circle at top center, rgba(140, 198, 255, 0.08), rgba(140, 198, 255, 0)),
    linear-gradient(180deg, rgba(255, 255, 255, 0.12), rgba(255, 255, 255, 0));
}

.chat-message {
  display: flex;
  gap: 14px;
}

.chat-message--user {
  flex-direction: row-reverse;
}

.chat-message__avatar {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  flex-shrink: 0;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.16), rgba(14, 165, 233, 0.2));
  color: #1d4ed8;
  font-weight: 700;
}

.chat-message__body {
  max-width: min(840px, 100%);
}

.chat-message__meta {
  gap: 10px;
  margin-bottom: 8px;
}

.chat-message__name {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.chat-message__bubble {
  border-radius: 22px;
  padding: 16px 18px;
  line-height: 1.8;
  word-break: break-word;
}

.chat-message__bubble--plain {
  background: linear-gradient(135deg, rgba(229, 238, 255, 0.96), rgba(240, 245, 255, 0.92));
  color: #0f172a;
  box-shadow: inset 0 0 0 1px rgba(198, 214, 255, 0.9);
  font-family:
    "PingFang SC",
    "Hiragino Sans GB",
    "Microsoft YaHei",
    "Segoe UI",
    sans-serif;
}

.chat-message__bubble--rich {
  background: rgba(255, 255, 255, 0.92);
  box-shadow:
    inset 0 0 0 1px rgba(226, 232, 240, 0.92),
    0 16px 36px rgba(15, 23, 42, 0.04);
}

.attachment-list,
.source-panel,
.composer__files {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.attachment-list,
.source-panel {
  margin-top: 12px;
}

.attachment-card,
.source-card,
.composer-file {
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.14);
}

.attachment-card {
  gap: 12px;
  padding: 12px 14px;
  color: #0f172a;
  text-decoration: none;
}

.attachment-card__icon,
.composer-file__icon {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: rgba(37, 99, 235, 0.1);
  color: #1d4ed8;
}

.attachment-card__content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.source-panel {
  flex-direction: column;
  align-items: flex-start;
}

.source-panel__title {
  gap: 8px;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 700;
}

.source-card {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  text-decoration: none;
  color: #0f172a;
}

.source-card__label {
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.1);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}

.source-card__title {
  font-size: 13px;
  font-weight: 600;
}

.composer {
  border-top: 1px solid rgba(226, 232, 240, 0.84);
  padding: 18px 22px 20px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(18px);
}

.composer__files {
  margin-bottom: 12px;
}

.composer-file {
  gap: 10px;
  padding: 8px 12px;
}

.composer-file__name {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.composer-file__remove {
  border: 0;
  background: transparent;
  color: #64748b;
  cursor: pointer;
}

.composer__input-wrap {
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow:
    inset 0 0 0 1px rgba(226, 232, 240, 0.94),
    0 16px 34px rgba(15, 23, 42, 0.04);
}

.composer__input {
  width: 100%;
  min-height: 96px;
  resize: none;
  border: 0;
  outline: none;
  padding: 18px 20px;
  background: transparent;
  color: #0f172a;
  font-size: 15px;
  line-height: 1.8;
}

.composer__toolbar {
  gap: 12px;
  margin-top: 12px;
}

.composer__toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.toolbar-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.82);
  color: #334155;
  box-shadow: inset 0 0 0 1px rgba(226, 232, 240, 0.92);
}

.toolbar-btn.is-disabled {
  opacity: 0.6;
  pointer-events: none;
}

.toolbar-btn__input {
  display: none;
}

.send-btn[disabled] {
  opacity: 0.6;
  cursor: not-allowed;
}

.send-btn {
  position: relative;
  min-width: 108px;
  height: 48px;
  padding: 0;
  overflow: hidden;
  border-radius: 18px;
  box-shadow:
    0 16px 30px rgba(37, 99, 235, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.28);
}

.send-btn__surface {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 100%;
  height: 100%;
  padding: 0 14px 0 18px;
  overflow: hidden;
}

.send-btn__label {
  position: relative;
  z-index: 2;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.01em;
}

.send-btn__icon-shell {
  position: relative;
  z-index: 2;
  width: 26px;
  height: 26px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.16);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.18);
}

.send-btn__ripples {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.send-btn__ripple {
  position: absolute;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.34);
  transform: scale(0);
  animation: send-btn-ripple 0.65s ease-out forwards;
}

.mobile-mask {
  display: none;
}

.markdown-body :deep(pre) {
  overflow: auto;
  border-radius: 16px;
}

.markdown-body {
  font-size: 12.5px;
  line-height: 1.88;
  color: #1f2937;
  font-family:
    "Noto Serif SC",
    "Source Han Serif SC",
    "Songti SC",
    "STSong",
    "SimSun",
    serif;
  font-weight: 500;
  letter-spacing: 0.01em;
  text-rendering: optimizeLegibility;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  margin: 0 0 12px;
  color: #0f172a;
  line-height: 1.28;
  letter-spacing: -0.02em;
  font-family:
    "SF Pro Display",
    "PingFang SC",
    "Hiragino Sans GB",
    "Microsoft YaHei",
    "Segoe UI",
    sans-serif;
}

.markdown-body :deep(h1) {
  font-size: 1.62rem;
}

.markdown-body :deep(h2) {
  font-size: 1.22rem;
}

.markdown-body :deep(h3) {
  font-size: 1.02rem;
}

.markdown-body :deep(h4) {
  font-size: 0.94rem;
  color: #334155;
}

.markdown-body :deep(p),
.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 0 0 12px;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 1.5em;
}

.markdown-body :deep(li) {
  margin-bottom: 8px;
  line-height: 1.84;
}

.markdown-body :deep(strong) {
  color: #0f172a;
  font-weight: 600;
}

.markdown-body :deep(blockquote) {
  margin: 0 0 16px;
  padding: 10px 14px;
  border-left: 3px solid rgba(79, 127, 255, 0.34);
  background: rgba(244, 247, 255, 0.7);
  color: #475569;
  border-radius: 0 14px 14px 0;
}

.markdown-body :deep(code):not(pre code) {
  padding: 2px 6px;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.06);
  color: #1d4ed8;
  font-size: 0.92em;
  font-family:
    "JetBrains Mono",
    "SFMono-Regular",
    "Consolas",
    monospace;
}

.markdown-body :deep(*:last-child) {
  margin-bottom: 0;
}

.ai-conversation.theme-minimal-light {
  grid-template-columns: 268px minmax(0, 1fr);
  column-gap: 20px;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  backdrop-filter: none;
}

.ai-conversation.theme-minimal-light.is-embedded {
  border-radius: 24px;
}

.ai-conversation.theme-minimal-light .ai-sidebar {
  border-right: 0;
  margin: 8px 0;
  border-radius: 30px;
  background: #ffffff;
  box-shadow:
    inset 0 0 0 1px rgba(15, 23, 42, 0.06),
    0 18px 36px rgba(15, 23, 42, 0.04);
}

.ai-conversation.theme-minimal-light .ai-shell {
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 0;
  height: 100%;
  background: transparent;
}

.ai-conversation.theme-minimal-light .session-card,
.ai-conversation.theme-minimal-light .starter-card,
.ai-conversation.theme-minimal-light .chat-message__bubble--rich,
.ai-conversation.theme-minimal-light .composer__input-wrap,
.ai-conversation.theme-minimal-light .attachment-card,
.ai-conversation.theme-minimal-light .source-card,
.ai-conversation.theme-minimal-light .toolbar-btn,
.ai-conversation.theme-minimal-light .ghost-btn,
.ai-conversation.theme-minimal-light .feature-chip,
.ai-conversation.theme-minimal-light .mobile-switcher__group,
.ai-conversation.theme-minimal-light .mobile-switcher__action {
  background: rgba(255, 255, 255, 0.96);
  box-shadow: inset 0 0 0 1px rgba(15, 23, 42, 0.07);
}

.ai-conversation.theme-minimal-light .session-card.is-active {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 248, 250, 0.98));
  box-shadow:
    inset 0 0 0 1px rgba(15, 23, 42, 0.14),
    0 10px 24px rgba(15, 23, 42, 0.06);
}

.ai-conversation.theme-minimal-light .ai-shell__header,
.ai-conversation.theme-minimal-light .composer {
  background: transparent;
  border: 0;
  backdrop-filter: none;
}

.ai-conversation.theme-minimal-light .ai-shell__header {
  width: min(100%, 1120px);
  margin: 0 auto;
  padding: 6px 8px 12px;
  align-items: flex-start;
  gap: 18px;
}

.ai-conversation.theme-minimal-light .ai-shell__title {
  flex: 1;
  min-width: 0;
  gap: 12px;
}

.ai-conversation.theme-minimal-light .ai-shell__home-btn {
  padding-right: 16px;
  padding-left: 16px;
}

.ai-conversation.theme-minimal-light .ai-shell__eyebrow {
  display: none;
}

.ai-conversation.theme-minimal-light .ai-shell__title h1 {
  max-width: min(340px, 100%);
  font-size: 22px;
  font-weight: 700;
  line-height: 1.24;
  letter-spacing: -0.04em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ai-conversation.theme-minimal-light .ai-shell__actions {
  flex-wrap: nowrap;
  gap: 12px;
}

.ai-conversation.theme-minimal-light .ai-messages {
  padding: 12px 32px 28px;
  background: transparent;
}

.ai-conversation.theme-minimal-light .ai-messages > * {
  width: min(100%, 920px);
  margin-right: auto;
  margin-left: auto;
}

.ai-conversation.theme-minimal-light .chat-message {
  width: min(100%, 920px);
}

.ai-conversation.theme-minimal-light .chat-message__body {
  max-width: min(720px, calc(100% - 54px));
}

.ai-conversation.theme-minimal-light .chat-message--user .chat-message__body {
  max-width: min(560px, calc(100% - 54px));
}

.ai-conversation.theme-minimal-light .composer,
.ai-conversation.theme-minimal-light.is-home-mode .composer {
  width: min(calc(100% - 48px), 980px);
  margin: 0 auto 16px;
  padding: 0;
  border: 0;
  box-shadow: none;
}

.ai-conversation.theme-minimal-light .feature-chip {
  color: #4b5563;
}

.ai-conversation.theme-minimal-light .feature-chip.is-active {
  background: #111827;
  color: #ffffff;
  box-shadow: none;
}

.ai-conversation.theme-minimal-light .feature-chip__hint,
.ai-conversation.theme-minimal-light .session-card__time,
.ai-conversation.theme-minimal-light .toolbar-tip,
.ai-conversation.theme-minimal-light .chat-message__time,
.ai-conversation.theme-minimal-light .attachment-card__content small {
  color: #9ca3af;
}

.ai-conversation.theme-minimal-light .feature-chip.is-active .feature-chip__hint {
  color: rgba(255, 255, 255, 0.72);
}

.ai-conversation.theme-minimal-light .ai-shell__mode-badge,
.ai-conversation.theme-minimal-light .source-card__label {
  background: #f3f4f6;
  color: #374151;
}

.ai-conversation.theme-minimal-light .ai-shell__mode-lock {
  color: #9ca3af;
}

.ai-conversation.theme-minimal-light .chat-message__avatar {
  background: #f3f4f6;
  color: #111827;
}

.ai-conversation.theme-minimal-light .chat-message__bubble--plain {
  background: #f5f7fa;
  box-shadow: inset 0 0 0 1px rgba(15, 23, 42, 0.06);
  color: #0f172a;
}

.ai-conversation.theme-minimal-light .chat-message__bubble--rich {
  background: #ffffff;
  box-shadow: inset 0 0 0 1px rgba(15, 23, 42, 0.08);
  color: #111827;
}

.ai-conversation.theme-minimal-light .composer__input-wrap {
  border-radius: 30px;
  background: #ffffff;
  box-shadow:
    inset 0 0 0 1px rgba(15, 23, 42, 0.08),
    0 18px 36px rgba(15, 23, 42, 0.08);
}

.ai-conversation.theme-minimal-light .composer__input {
  min-height: 116px;
  padding: 22px 24px 18px;
  font-size: 16px;
  line-height: 1.72;
}

.ai-conversation.theme-minimal-light .composer__toolbar {
  margin-top: 0;
  padding: 0 10px 10px;
}

.ai-conversation.theme-minimal-light .composer__toolbar-left {
  gap: 10px;
}

.ai-conversation.theme-minimal-light .toolbar-btn {
  padding: 10px 16px;
}

.ai-conversation.theme-minimal-light .send-btn {
  min-width: 132px;
  height: 60px;
  border-radius: 999px;
}

.ai-conversation.theme-minimal-light .empty-state {
  min-height: 100%;
  padding-top: 7vh;
  padding-bottom: 11vh;
}

.ai-conversation.theme-minimal-light .starter-grid {
  max-width: 860px;
}

.ai-conversation.theme-minimal-light .feature-chip--mode {
  min-width: 132px;
  padding: 12px 14px 12px 12px;
}

.ai-conversation.theme-minimal-light .feature-chip--ghost {
  height: 56px;
  padding: 0 20px;
}

.ai-conversation.theme-minimal-light .composer__input::placeholder {
  color: #9ca3af;
}

.ai-conversation.theme-minimal-light .send-btn {
  background: linear-gradient(135deg, #111827, #1f2937);
  box-shadow:
    0 12px 24px rgba(15, 23, 42, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.ai-conversation.theme-minimal-light .send-btn__icon-shell {
  background: rgba(255, 255, 255, 0.1);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.1);
}

.ai-conversation.theme-minimal-light .markdown-body,
.ai-conversation.theme-minimal-light .markdown-body :deep(h1),
.ai-conversation.theme-minimal-light .markdown-body :deep(h2),
.ai-conversation.theme-minimal-light .markdown-body :deep(h3),
.ai-conversation.theme-minimal-light .markdown-body :deep(h4),
.ai-conversation.theme-minimal-light .chat-message__name,
.ai-conversation.theme-minimal-light .feature-chip__label {
  color: #111827;
}

.ai-conversation.theme-minimal-light .markdown-body :deep(blockquote) {
  background: #f8fafc;
  border-left-color: rgba(15, 23, 42, 0.16);
  color: #475569;
}

.ai-conversation.theme-minimal-light .markdown-body :deep(code):not(pre code) {
  background: #f3f4f6;
  color: #1f2937;
}

.ai-conversation.theme-minimal-light .ai-shell__eyebrow {
  color: #9ca3af;
}

.ai-conversation.theme-minimal-light .mobile-switcher__group,
.ai-conversation.theme-minimal-light .mobile-switcher__action {
  background: #ffffff;
  box-shadow:
    inset 0 0 0 1px rgba(15, 23, 42, 0.08),
    0 10px 20px rgba(15, 23, 42, 0.05);
}

@keyframes send-btn-ripple {
  0% {
    transform: scale(0);
    opacity: 0.9;
  }
  100% {
    transform: scale(1);
    opacity: 0;
  }
}

@media (max-width: 1100px) {
  .ai-conversation:not(.is-embedded) {
    grid-template-columns: minmax(0, 1fr);
    height: 100%;
    min-height: 0;
  }

  .ai-sidebar {
    position: fixed;
    inset: 0 auto 0 0;
    width: min(320px, 86vw);
    transform: translateX(-100%);
    transition: transform 0.24s ease;
    z-index: 1002;
  }

  .ai-sidebar.is-open {
    transform: translateX(0);
  }

  .mobile-toggle,
  .mobile-mask {
    display: block;
  }

  .mobile-mask {
    position: fixed;
    inset: 0;
    background: rgba(15, 23, 42, 0.3);
    z-index: 1001;
  }
}

@media (max-width: 768px) {
  .ai-conversation {
    height: 100%;
    min-height: 0;
    border-radius: 18px;
  }

  .ai-conversation.is-home-mode .composer {
    width: calc(100% - 10px);
    margin-bottom: 6px;
    border-radius: 18px;
  }

  .ai-shell__header,
  .ai-messages,
  .composer {
    padding-left: 6px;
    padding-right: 6px;
  }

  .mobile-session-panel {
    padding: 2px 4px 8px;
  }

  .mobile-session-panel__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
    padding: 0 8px;
    color: #64748b;
    font-size: 11px;
  }

  .mobile-session-panel__list {
    max-height: calc(100vh - 136px);
    padding-right: 0;
  }

  .feature-chip,
  .ghost-btn,
  .toolbar-btn,
  .send-btn {
    font-size: 11px;
  }

  .feature-chip {
    flex: 0 0 auto;
    padding: 7px 9px;
    gap: 5px;
  }

  .feature-chip--mode {
    min-width: 122px;
    padding: 8px 10px;
  }

  .feature-chip__label {
    font-size: 12px;
  }

  .feature-chip__hint {
    font-size: 10px;
  }

  .ai-shell__mode-banner {
    margin: 0 10px;
    padding: 12px;
    border-radius: 18px;
    gap: 10px;
  }

  .ai-shell__mode-icon {
    width: 38px;
    height: 38px;
    border-radius: 13px;
  }

  .ai-shell__mode-copy strong {
    font-size: 15px;
  }

  .ai-shell__mode-copy p {
    margin-top: 4px;
    font-size: 11px;
    line-height: 1.55;
  }

  .ai-shell__mode-badge,
  .ai-shell__mode-lock {
    height: 22px;
    padding: 0 8px;
    font-size: 10px;
  }

  .ai-messages {
    padding-top: 2px;
    padding-bottom: 6px;
    gap: 10px;
  }

  .starter-grid {
    grid-template-columns: 1fr;
    gap: 10px;
    margin-top: 18px;
  }

  .starter-card {
    border-radius: 18px;
    padding: 14px 12px;
  }

  .starter-card strong {
    font-size: 15px;
  }

  .starter-card span {
    font-size: 12px;
  }

  .chat-message__body {
    max-width: calc(100% - 46px);
  }

  .chat-message {
    gap: 10px;
  }

  .chat-message__avatar {
    width: 32px;
    height: 32px;
    border-radius: 11px;
    font-size: 11px;
  }

  .chat-message__meta {
    margin-bottom: 4px;
  }

  .chat-message__name,
  .source-panel__title,
  .source-card__title,
  .composer-file__name {
    font-size: 11px;
  }

  .chat-message__time,
  .source-card__label,
  .toolbar-tip,
  .attachment-card__content small {
    font-size: 10px;
  }

  .chat-message__bubble {
    padding: 9px 11px;
    border-radius: 14px;
    line-height: 1.5;
    font-size: 11px;
  }

  .markdown-body {
    font-size: 11.5px;
    line-height: 1.8;
  }

  .markdown-body :deep(h1) {
    font-size: 1.4rem;
  }

  .markdown-body :deep(h2) {
    font-size: 1.12rem;
  }

  .markdown-body :deep(h3) {
    font-size: 0.96rem;
  }

  .composer {
    position: relative;
    padding: 8px 6px 6px;
    border-top: 0;
    background: transparent;
    backdrop-filter: none;
  }

  .composer__input {
    min-height: 56px;
    max-height: 112px;
    padding: 10px 12px;
    font-size: 11px;
    line-height: 1.48;
  }

  .mobile-mode-menu {
    position: absolute;
    right: 6px;
    bottom: calc(100% + 8px);
    left: 6px;
    z-index: 12;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
    padding: 10px;
    border-radius: 24px;
    background: rgba(255, 255, 255, 0.62);
    box-shadow:
      inset 0 0 0 1px rgba(255, 255, 255, 0.42),
      0 18px 38px rgba(15, 23, 42, 0.12);
    backdrop-filter: blur(24px) saturate(150%);
  }

  .mobile-mode-menu__item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 12px;
    border: 0;
    border-radius: 18px;
    background: rgba(255, 255, 255, 0.54);
    color: #334155;
    text-align: left;
    box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.16);
  }

  .mobile-mode-menu__item.is-active {
    background: linear-gradient(135deg, rgba(37, 99, 235, 0.16), rgba(125, 211, 252, 0.18));
    color: #0f172a;
    box-shadow:
      inset 0 0 0 1px rgba(59, 130, 246, 0.22),
      0 10px 22px rgba(37, 99, 235, 0.1);
  }

  .mobile-mode-menu__icon {
    width: 34px;
    height: 34px;
    border-radius: 12px;
    display: grid;
    place-items: center;
    background: rgba(255, 255, 255, 0.76);
    color: #2563eb;
    flex-shrink: 0;
  }

  .mobile-mode-menu__copy {
    display: flex;
    flex-direction: column;
    min-width: 0;
    gap: 2px;
  }

  .mobile-mode-menu__copy strong {
    font-size: 12px;
    line-height: 1.2;
  }

  .mobile-mode-menu__copy small {
    color: #64748b;
    font-size: 10px;
    line-height: 1.3;
  }

  .mobile-composer__row {
    display: flex;
    align-items: flex-end;
    gap: 8px;
  }

  .mobile-mode-trigger,
  .mobile-icon-btn,
  .mobile-send-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    border: 0;
    background: rgba(255, 255, 255, 0.6);
    color: #0f172a;
    backdrop-filter: blur(22px) saturate(160%);
    box-shadow:
      inset 0 0 0 1px rgba(255, 255, 255, 0.45),
      0 14px 30px rgba(15, 23, 42, 0.1);
  }

  .mobile-mode-trigger {
    gap: 8px;
    min-width: 72px;
    height: 52px;
    padding: 0 12px;
    border-radius: 20px;
    align-items: center;
  }

  .mobile-mode-trigger__orb {
    display: inline-grid;
    place-items: center;
    width: 28px;
    height: 28px;
    border-radius: 11px;
    background: linear-gradient(135deg, rgba(96, 165, 250, 0.28), rgba(186, 230, 253, 0.36));
    color: #1d4ed8;
  }

  .mobile-mode-trigger__label {
    max-width: 44px;
    overflow: hidden;
    font-size: 11px;
    font-weight: 700;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .mobile-mode-trigger__chevron {
    font-size: 10px;
    color: #64748b;
    transition: transform 0.2s ease;
  }

  .mobile-mode-trigger.is-open .mobile-mode-trigger__chevron {
    transform: rotate(180deg);
  }

  .composer__input-wrap--mobile {
    flex: 1;
    min-width: 0;
    border-radius: 24px;
    background: rgba(255, 255, 255, 0.72);
    box-shadow:
      inset 0 0 0 1px rgba(255, 255, 255, 0.44),
      0 16px 32px rgba(15, 23, 42, 0.09);
    backdrop-filter: blur(24px) saturate(160%);
  }

  .composer__input--mobile {
    min-height: 52px;
    max-height: 100px;
    padding: 14px 16px;
    font-size: 13px;
    line-height: 1.45;
  }

  .mobile-icon-btn,
  .mobile-send-btn {
    position: relative;
    width: 52px;
    height: 52px;
    border-radius: 18px;
    cursor: pointer;
    overflow: hidden;
  }

  .mobile-send-btn {
    background: linear-gradient(135deg, rgba(37, 99, 235, 0.88), rgba(59, 130, 246, 0.92));
    color: #fff;
  }

  .mobile-send-btn[disabled],
  .mobile-icon-btn.is-disabled {
    opacity: 0.55;
    pointer-events: none;
  }

  .attachment-card,
  .source-card,
  .composer-file {
    border-radius: 11px;
  }

  .attachment-card {
    gap: 7px;
    padding: 9px 10px;
  }

  .attachment-card__icon,
  .composer-file__icon {
    width: 26px;
    height: 26px;
    border-radius: 9px;
    font-size: 11px;
  }

  .toolbar-tip {
    color: #64748b;
  }
}

@media (max-width: 380px) {
  .mobile-mode-trigger {
    min-width: 62px;
    padding: 0 10px;
  }

  .mobile-mode-trigger__label {
    display: none;
  }

  .mobile-mode-menu {
    grid-template-columns: 1fr;
  }
}
</style>
