<template>
  <div class="message-page" :style="cover">
    <section class="message-hero">
      <div class="message-copy">
        <span class="message-eyebrow">Message Board</span>
        <h1>留言板</h1>
        <p class="message-summary">
          留下一句想说的话，留言会以弹幕的形式穿过页面。手机端输入区改成了更稳的双层结构，不再挤在标题下面。
        </p>

        <div class="message-stats">
          <article class="message-stat">
            <strong>{{ barrageList.length }}</strong>
            <span>当前留言</span>
          </article>
          <article class="message-stat">
            <strong>{{ countdownText }}</strong>
            <span>下一次发送</span>
          </article>
          <article class="message-stat">
            <strong>{{ identityLabel }}</strong>
            <span>当前身份</span>
          </article>
        </div>
      </div>

      <div class="composer-card">
        <label class="composer-label">说点什么</label>
        <el-input
          v-model="content"
          class="composer-input"
          type="textarea"
          :autosize="{ minRows: 3, maxRows: 5 }"
          resize="none"
          placeholder="想分享一句日常、灵感或者路过的心情都可以"
          @keyup.enter.native="handleTextareaEnter"
        />
        <div class="composer-footer">
          <span class="composer-tip">
            {{ count ? `${count} 秒后可再次发送` : '发送后会实时加入弹幕墙' }}
          </span>
          <el-button
            type="primary"
            round
            :disabled="submitDisabled"
            @click="addToList"
          >
            发送留言
          </el-button>
        </div>
      </div>
    </section>

    <section class="barrage-panel">
      <div class="panel-header">
        <div>
          <p class="panel-eyebrow">Live Wall</p>
          <h2>实时弹幕</h2>
        </div>
        <div class="panel-status">
          <span class="status-dot"></span>
          <span>实时更新中</span>
        </div>
      </div>

      <div v-if="!barrageList.length" class="empty-panel">
        <i class="fas fa-comment-dots"></i>
        <div>
          <strong>还没有留言</strong>
          <p>第一条留言会从这里开始出现。</p>
        </div>
      </div>

      <div v-else class="barrage-stage">
        <vue-danmaku
          class="danmaku"
          :danmus="barrageList"
          style="height: 100%; width: 100%"
          useSlot
          :speeds="130"
          :channels="isMobile ? 10 : 14"
        >
          <template v-slot:dm="{ danmu }">
            <span class="barrage-item">
              <img :src="resolveAvatar(danmu.avatar)" alt="avatar">
              <span class="barrage-body">
                <span class="barrage-meta">
                  <span class="barrage-name">{{ danmu.nickname || '游客' }}</span>
                  <time v-if="formatMessageTime(danmu.createTime)" class="barrage-time">
                    {{ formatMessageTime(danmu.createTime) }}
                  </time>
                </span>
                <span class="barrage-content">{{ danmu.content }}</span>
              </span>
            </span>
          </template>
        </vue-danmaku>
      </div>
    </section>
  </div>
</template>

<script>
import { getMessagesApi, addMessageApi } from '@/api/message'
import { formatDateTime } from '@/utils/time'
import { resolveImageUrl } from '@/utils/image'
import VueDanmaku from 'vue-danmaku'

export default {
  name: 'MessagesView',
  components: {
    VueDanmaku
  },
  data() {
    return {
      content: '',
      count: null,
      timer: null,
      barrageList: [],
      isMobile: typeof window !== 'undefined' ? window.innerWidth <= 768 : false
    }
  },
  computed: {
    user() {
      return this.$store.state.userInfo
    },
    submitDisabled() {
      return !this.content.trim() || Boolean(this.count)
    },
    countdownText() {
      return this.count ? `${this.count}s` : '可发送'
    },
    identityLabel() {
      return this.user?.nickname || '游客'
    },
    cover() {
      return {
        background:
          'radial-gradient(circle at 20% 20%, rgba(96, 165, 250, 0.16), transparent 24%), ' +
          'radial-gradient(circle at 85% 8%, rgba(129, 140, 248, 0.18), transparent 28%), ' +
          'linear-gradient(160deg, #0f172a 0%, #1d4ed8 52%, #38bdf8 100%)'
      }
    }
  },
  mounted() {
    this.listMessage()
    window.addEventListener('resize', this.syncViewport)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.syncViewport)
    if (this.timer) {
      clearInterval(this.timer)
      this.timer = null
    }
  },
  methods: {
    syncViewport() {
      this.isMobile = window.innerWidth <= 768
    },
    handleTextareaEnter(event) {
      if (event.shiftKey) {
        return
      }
      event.preventDefault()
      this.addToList()
    },
    startCooldown() {
      const timeCount = 30
      if (this.timer) {
        return
      }

      this.count = timeCount
      this.timer = setInterval(() => {
        if (this.count > 1) {
          this.count -= 1
          return
        }

        clearInterval(this.timer)
        this.timer = null
        this.count = null
      }, 1000)
    },
    async addToList() {
      if (this.count) {
        this.$message.error('30 秒后才能再次留言')
        return
      }

      const text = this.content.trim()
      if (!text) {
        this.$message.error('留言内容不能为空')
        return
      }

      const message = {
        avatar: this.user ? this.user.avatar : this.$store.state.webSiteInfo.touristAvatar,
        status: 1,
        nickname: this.user ? this.user.nickname : '游客',
        content: text,
        createTime: new Date().toISOString()
      }

      this.content = ''

      try {
        await addMessageApi(message)
        this.barrageList.push(message)
        this.$message.success('留言成功')
        this.startCooldown()
      } catch (error) {
        this.$message.error(error.message || '留言失败')
      }
    },
    listMessage() {
      getMessagesApi().then((res) => {
        this.barrageList = Array.isArray(res.data) ? res.data : []
      }).catch(() => {
        this.barrageList = []
      })
    },
    formatMessageTime(time) {
      return formatDateTime(time)
    },
    resolveAvatar(url) {
      return resolveImageUrl(url, this.$store.state.webSiteInfo.touristAvatar || this.$store.state.defaultImage)
    }
  }
}
</script>

<style lang="scss" scoped>
.message-page {
  min-height: calc(100vh - 80px);
  padding: 24px 20px 88px;
}

.message-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(320px, 0.92fr);
  gap: 24px;
  max-width: 1240px;
  margin: 0 auto 24px;
}

.message-copy,
.composer-card,
.barrage-panel {
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 30px;
  background: rgba(9, 18, 43, 0.42);
  backdrop-filter: blur(18px);
  box-shadow: 0 22px 50px rgba(2, 8, 23, 0.28);
}

.message-copy,
.composer-card,
.barrage-panel {
  color: #fff;
}

.message-copy {
  padding: 34px;
}

.message-eyebrow,
.panel-eyebrow,
.composer-label {
  display: inline-flex;
  color: rgba(191, 219, 254, 0.92);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.message-copy h1,
.panel-header h2 {
  margin: 14px 0 14px;
  font-size: clamp(34px, 7vw, 60px);
  line-height: 1;
  font-weight: 800;
}

.message-summary {
  max-width: 560px;
  margin: 0;
  color: rgba(226, 232, 240, 0.88);
  font-size: 16px;
  line-height: 1.85;
}

.message-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 28px;
}

.message-stat {
  padding: 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);

  strong {
    display: block;
    margin-bottom: 8px;
    color: #fff;
    font-size: 28px;
    font-weight: 800;
  }

  span {
    color: rgba(191, 219, 254, 0.82);
    font-size: 13px;
    line-height: 1.6;
  }
}

.composer-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 28px;
}

.composer-input {
  :deep(.el-textarea__inner) {
    min-height: 148px !important;
    padding: 16px 18px;
    border: 1px solid rgba(255, 255, 255, 0.14);
    border-radius: 22px;
    background: rgba(255, 255, 255, 0.1);
    color: #fff;
    font-size: 15px;
    line-height: 1.8;

    &::placeholder {
      color: rgba(226, 232, 240, 0.62);
    }

    &:focus {
      border-color: rgba(125, 211, 252, 0.46);
      box-shadow: 0 0 0 4px rgba(56, 189, 248, 0.08);
    }
  }
}

.composer-footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;

  :deep(.el-button) {
    min-width: 126px;
    height: 44px;
  }
}

.composer-tip {
  color: rgba(191, 219, 254, 0.82);
  font-size: 13px;
  line-height: 1.6;
}

.barrage-panel {
  max-width: 1240px;
  margin: 0 auto;
  padding: 28px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: end;
  margin-bottom: 18px;

  h2 {
    margin: 10px 0 0;
    font-size: clamp(24px, 4vw, 32px);
  }
}

.panel-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: rgba(191, 219, 254, 0.86);
  font-size: 13px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 8px rgba(34, 197, 94, 0.12);
}

.empty-panel {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 22px;
  border-radius: 24px;
  border: 1px dashed rgba(255, 255, 255, 0.14);
  color: rgba(226, 232, 240, 0.86);

  i {
    font-size: 24px;
    color: #93c5fd;
  }

  strong {
    display: block;
    margin-bottom: 4px;
    color: #fff;
  }

  p {
    margin: 0;
    line-height: 1.6;
  }
}

.barrage-stage {
  height: 460px;
  overflow: hidden;
  border-radius: 26px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background:
    radial-gradient(circle at 50% 0%, rgba(125, 211, 252, 0.12), transparent 42%),
    rgba(5, 11, 29, 0.3);
}

.barrage-item {
  display: flex;
  align-items: center;
  gap: 10px;
  max-width: 440px;
  margin-top: 10px;
  padding: 8px 14px 8px 8px;
  color: #fff;
  background: rgba(15, 23, 42, 0.86);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 999px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.24);

  img {
    width: 34px;
    height: 34px;
    border-radius: 50%;
    object-fit: cover;
    flex-shrink: 0;
  }
}

.barrage-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.barrage-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.barrage-name {
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}

.barrage-time {
  color: rgba(191, 219, 254, 0.74);
  font-size: 11px;
  white-space: nowrap;
}

.barrage-content {
  color: rgba(226, 232, 240, 0.92);
  font-size: 13px;
  line-height: 1.4;
  word-break: break-word;
}

@media (max-width: 1024px) {
  .message-hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .message-page {
    min-height: auto;
    padding: 14px 12px 64px;
  }

  .message-copy,
  .composer-card,
  .barrage-panel {
    border-radius: 24px;
    padding: 20px 16px;
  }

  .message-stats {
    grid-template-columns: 1fr;
  }

  .composer-footer,
  .panel-header {
    flex-direction: column;
    align-items: stretch;
  }

  .composer-tip {
    order: 2;
  }

  .barrage-stage {
    height: 54vh;
    min-height: 380px;
  }

  .barrage-item {
    max-width: 320px;
  }
}
</style>
